package pl.rentowne.rental_document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pl.rentowne.common.mail.AttachmentEmailService;
import pl.rentowne.common.mail.DocumentMail;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class DocumentDeliveryWorker {
    private final DocumentStore store;
    private final DocumentProperties properties;
    private final DocumentFileStore files;
    private final RentalDocumentPdfService pdf;
    private final AttachmentEmailService mail;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;

    public DocumentDeliveryWorker(DocumentStore store, DocumentProperties properties, DocumentFileStore files,
                                  RentalDocumentPdfService pdf, AttachmentEmailService mail, ObjectMapper json,
                                  PlatformTransactionManager manager) {
        this.store = store;
        this.properties = properties;
        this.files = files;
        this.pdf = pdf;
        this.mail = mail;
        this.json = json;
        this.transactions = new TransactionTemplate(manager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean processNext() {
        Optional<DocumentStore.Delivery> claimed = transactions.execute(status -> store.claim(properties));
        if (claimed == null || claimed.isEmpty()) return false;
        DocumentStore.Delivery delivery = claimed.get();
        boolean smtpMarked = false;
        try {
            DocumentStore.Stored document = store.document(delivery.documentId(), false);
            if (document.cancelled() && delivery.kind().equals("ISSUE")) {
                complete(delivery, "CANCELLED", null);
                return true;
            }
            RentalDocumentData data = json.readValue(document.snapshot(), RentalDocumentData.class);
            byte[] attachment = null;
            if (delivery.kind().equals("ISSUE")) {
                if (document.filename() == null) {
                    if (document.templateVersion() != 1) throw new IllegalStateException("Nieobsługiwana wersja szablonu");
                    byte[] bytes = pdf.generate(data);
                    Boolean saved = transactions.execute(status -> {
                        var locked = store.document(document.id(), true);
                        var current = store.delivery(delivery.id(), true);
                        if (!store.owns(current, delivery) || locked.cancelled()) return false;
                        if (locked.filename() == null) {
                            try {
                                String filename = files.write(document.id(), bytes);
                                store.savePdf(document.id(), filename, DocumentFileStore.hash(bytes));
                            } catch (IOException error) { throw new UncheckedIOException(error); }
                        }
                        return true;
                    });
                    if (!Boolean.TRUE.equals(saved)) return true;
                }
                DocumentStore.Stored ready = store.document(document.id(), false);
                attachment = files.read(ready.filename(), ready.checksum());
            }
            if (!delivery.sendEnabled()) {
                complete(delivery, "HELD", "Wysyłka rozliczeń kaucji oczekuje na weryfikację obliczeń końcowych.");
                return true;
            }
            DocumentMail message = message(delivery, data, attachment);
            // This committed marker prevents a blind retry after an interrupted SMTP exchange.
            smtpMarked = Boolean.TRUE.equals(transactions.execute(status -> store.markSmtp(delivery)));
            if (!smtpMarked) return true;
            transactions.executeWithoutResult(status -> {
                // Cancellation and dispatch take locks in the same order. SMTP has bounded timeouts.
                DocumentStore.Stored locked = store.document(delivery.documentId(), true);
                DocumentStore.Delivery current = store.delivery(delivery.id(), true);
                if (!store.owns(current, delivery)) return;
                if (locked.cancelled() && delivery.kind().equals("ISSUE")) {
                    store.finish(delivery, "CANCELLED", null);
                    return;
                }
                try {
                    store.finish(delivery, mail.send(message).name(), null);
                } catch (MailException error) {
                    String outcome = smtpOutcome(error);
                    if (outcome.equals("PENDING") && delivery.attempts() >= properties.maxAttempts()) outcome = "FAILED";
                    store.finish(delivery, outcome, errorMessage(error));
                    log.warn("Document delivery {} ended with {} ({})", delivery.id(), outcome, error.getClass().getSimpleName());
                }
            });
        } catch (IOException | RuntimeException error) {
            String outcome = smtpMarked ? "UNKNOWN" : delivery.attempts() < properties.maxAttempts() ? "PENDING" : "FAILED";
            complete(delivery, outcome, errorMessage(error));
            log.warn("Document task {} ended with {} ({})", delivery.id(), outcome, error.getClass().getSimpleName());
        }
        return true;
    }

    private void complete(DocumentStore.Delivery delivery, String state, String error) {
        transactions.executeWithoutResult(status -> store.finish(delivery, state, error));
    }

    private DocumentMail message(DocumentStore.Delivery delivery, RentalDocumentData data, byte[] attachment) throws IOException {
        List<String> cc = json.readValue(delivery.ccJson(), new TypeReference<>() {});
        boolean cancellation = delivery.kind().equals("CANCELLATION");
        String subject = (cancellation ? "Anulowanie - " : "") + data.type().title() + " " + data.number()
                + (data.type() == RentalDocumentType.RENT_RECEIPT ? " - " + data.period() : "");
        String body = cancellation
                ? "Dzień dobry,\n\nrachunek nr " + data.number() + " został anulowany i nie stanowi podstawy płatności."
                + " Jeśli wymagane będzie nowe rozliczenie, otrzymają Państwo osobny dokument."
                : "Dzień dobry,\n\nw załączniku przesyłamy " + data.type().title().toLowerCase(java.util.Locale.forLanguageTag("pl-PL"))
                + " nr " + data.number() + ".\nLokal / obiekt: " + data.propertyAddress()
                + (data.type() == RentalDocumentType.RENT_RECEIPT ? "\nOkres rozliczeniowy: " + data.period() : "")
                + ".\nSzczegóły rozliczenia znajdują się w dokumencie PDF.";
        body += "\n\nZ poważaniem,\nRentowne Nieruchomości";
        String filename = data.type().filePrefix() + "-" + data.number().replace('/', '-') + ".pdf";
        return new DocumentMail(delivery.to(), cc, subject, body, cancellation ? null : filename, attachment);
    }

    private static String smtpOutcome(MailException error) {
        if (error instanceof MailAuthenticationException || error instanceof MailPreparationException) return "FAILED";
        var pending = new ArrayDeque<Throwable>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.add(error);
        boolean retry = false, rejected = false;
        while (!pending.isEmpty()) {
            Throwable item = pending.removeFirst();
            if (!seen.add(item)) continue;
            if (item instanceof SendFailedException failed && failed.getValidSentAddresses() != null
                    && failed.getValidSentAddresses().length > 0) return "UNKNOWN";
            if (item instanceof ConnectException || item instanceof UnknownHostException) retry = true;
            int code = item instanceof SMTPSendFailedException failed ? failed.getReturnCode()
                    : item instanceof SMTPAddressFailedException failed ? failed.getReturnCode() : 0;
            if (code >= 400 && code < 500) retry = true;
            if (code >= 500 && code < 600) rejected = true;
            if (item.getCause() != null) pending.add(item.getCause());
            if (item instanceof MessagingException messaging && messaging.getNextException() != null) pending.add(messaging.getNextException());
            if (item instanceof MailSendException send) Collections.addAll(pending, send.getMessageExceptions());
        }
        return rejected ? "FAILED" : retry ? "PENDING" : "UNKNOWN";
    }

    private static String errorMessage(Throwable error) {
        String message = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage()).replaceAll("[\\r\\n]", " ");
        return message.substring(0, Math.min(message.length(), 1000));
    }
}
