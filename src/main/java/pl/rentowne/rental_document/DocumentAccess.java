package pl.rentowne.rental_document;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import pl.rentowne.rental_payment.RentalPaymentException;
import pl.rentowne.user.UserService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class DocumentAccess {
    private final RentalDocumentRepository documents;
    private final DocumentDeliveryRepository deliveries;
    private final UserService users;

    RentalDocument owned(UUID id, boolean lock) {
        Long userId = users.getLoggedUser().id();
        return (lock ? documents.lockOwned(id, userId) : documents.findByIdAndUserId(id, userId))
                .orElseThrow(
                        () ->
                                new RentalPaymentException(
                                        HttpStatus.NOT_FOUND, "Nie znaleziono dokumentu."));
    }

    void checkRevision(RentalDocument document, long revision) {
        if (document.getRevision() != revision)
            throw conflict("Dokument zmienił się. Odśwież podgląd.");
    }

    void mutable(RentalDocument document) {
        if (deliveries.protectsFile(document.getId()))
            throw conflict(
                    "Nie można zmienić ani usunąć PDF wysłanego lub z niepewnym wynikiem wysyłki.");
    }

    static RentalPaymentException conflict(String message) {
        return new RentalPaymentException(HttpStatus.CONFLICT, message);
    }
}
