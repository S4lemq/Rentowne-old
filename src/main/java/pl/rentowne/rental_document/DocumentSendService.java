package pl.rentowne.rental_document;

import static pl.rentowne.rental_document.DocumentAccess.conflict;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import pl.rentowne.common.mail.AttachmentEmailService;
import pl.rentowne.rental_payment.RentalPaymentException;
import pl.rentowne.user.UserService;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

@Service
class DocumentSendService {
    private final DocumentDeliveryRepository deliveries;
    private final DocumentAccess access;
    private final DocumentMapper mapper;
    private final DocumentMessageFactory messages;
    private final DocumentFileStore files;
    private final AttachmentEmailService mail;
    private final UserService users;
    private final TransactionTemplate tx;

    DocumentSendService(
            DocumentDeliveryRepository deliveries,
            DocumentAccess access,
            DocumentMapper mapper,
            DocumentFileStore files,
            AttachmentEmailService mail,
            UserService users,
            DocumentMessageFactory messages,
            PlatformTransactionManager manager) {
        this.deliveries = deliveries;
        this.access = access;
        this.mapper = mapper;
        this.files = files;
        this.mail = mail;
        this.users = users;
        this.messages = messages;
        tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverInterruptedSends() {
        deliveries.recoverInterrupted(OffsetDateTime.now(), "Przerwano aplikację podczas wysyłki.");
    }

    public void send(UUID id, DocumentSendRequest request) {
        Long user = users.getLoggedUser().id();
        UUID delivery =
                tx.execute(
                        status -> {
                            var d = access.owned(id, true);
                            if (deliveries.existsByDocumentIdAndRequestId(id, request.requestId()))
                                return null;
                            access.checkRevision(d, request.revision());
                            if (!request.confirmed())
                                throw conflict("Potwierdź sprawdzenie dokumentu i odbiorców.");
                            if (!Objects.equals(d.getChecksum(), request.checksum()))
                                throw conflict("PDF zmienił się. Otwórz podgląd ponownie.");
                            var history = deliveries.findByDocumentIdOrderByCreatedAtDescIdDesc(id);
                            if (history.stream()
                                    .anyMatch(
                                            h -> h.getState() == DocumentDeliveryState.PROCESSING))
                                throw conflict("Wysyłka już trwa.");
                            if (history.stream()
                                            .anyMatch(
                                                    h ->
                                                            h.getState()
                                                                    == DocumentDeliveryState
                                                                            .UNKNOWN)
                                    && !request.acknowledgeUnknown())
                                throw conflict(
                                        "Poprzednia wiadomość mogła zostać wysłana. Potwierdź"
                                            + " ryzyko duplikatu.");
                            if (request.cancellation()) {
                                if (!d.isCancelled() || !deliveries.protectsFile(id))
                                    throw conflict("Brak wysłanego, anulowanego dokumentu.");
                            } else if (d.isCancelled() || d.getFilename() == null)
                                throw conflict("Dokument jest anulowany lub nie ma PDF.");
                            String to = address(request.to());
                            List<String> cc =
                                    request.cc().stream()
                                            .map(DocumentSendService::address)
                                            .distinct()
                                            .filter(a -> !a.equals(to))
                                            .toList();
                            var attempt =
                                    DocumentDelivery.start(d, request, user, to, mapper.encode(cc));
                            deliveries.saveAndFlush(attempt);
                            return attempt.getId();
                        });
        if (delivery == null) return;
        try {
            tx.executeWithoutResult(
                    status -> {
                        var d =
                                access.owned(
                                        id,
                                        true); // Same lock used by
                                               // cancellation/regeneration/deletion.
                        if (!request.cancellation() && d.isCancelled()) {
                            finish(delivery, "CANCELLED", null);
                            return;
                        }
                        var h = deliveries.findById(delivery).orElseThrow();
                        try {
                            byte[] attachment =
                                    request.cancellation()
                                            ? null
                                            : files.read(d.getFilename(), d.getChecksum());
                            String result = mail.send(messages.create(d, h, attachment)).name();
                            finish(delivery, result, null);
                        } catch (IOException error) {
                            finish(delivery, "FAILED", "Nie można odczytać zapisanego PDF.");
                        } catch (MailException error) {
                            finish(
                                    delivery,
                                    DocumentMailOutcome.outcome(error),
                                    "Błąd wysyłki: "
                                            + error.getClass().getSimpleName()
                                            + ". Sprawdź pocztę i konfigurację SMTP.");
                        }
                    });
        } catch (RuntimeException error) {
            tx.executeWithoutResult(
                    status ->
                            finish(
                                    delivery,
                                    "UNKNOWN",
                                    "Nie potwierdzono wyniku wysyłki. Sprawdź pocztę przed"
                                        + " ponowieniem."));
        }
    }

    private void finish(UUID id, String state, String error) {
        deliveries.findById(id).orElseThrow().finish(DocumentDeliveryState.valueOf(state), error);
    }

    private static String address(String value) {
        try {
            if (value == null || value.contains("\r") || value.contains("\n"))
                throw new AddressException();
            String clean = value.strip().toLowerCase(Locale.ROOT);
            new InternetAddress(clean, true).validate();
            return clean;
        } catch (AddressException error) {
            throw new RentalPaymentException(HttpStatus.BAD_REQUEST, "Niepoprawny adres e-mail.");
        }
    }
}
