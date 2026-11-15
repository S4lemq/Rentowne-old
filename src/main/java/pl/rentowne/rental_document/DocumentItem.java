package pl.rentowne.rental_document;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentItem(
        UUID id,
        RentalDocumentData data,
        String status,
        boolean cancelled,
        boolean hasPdf,
        String mailState,
        OffsetDateTime lastSentAt) {}
