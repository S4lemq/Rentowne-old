package pl.rentowne.rental_document;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DocumentDetail(
        UUID id,
        RentalDocumentData data,
        boolean cancelled,
        OffsetDateTime cancelledAt,
        boolean hasPdf,
        String checksum,
        long revision,
        String generationError,
        String to,
        List<String> cc,
        boolean canRegenerate,
        boolean canDelete,
        boolean canSend,
        boolean canNotifyCancellation,
        String status,
        List<DocumentHistory> history,
        Long settlementId,
        Long leaseId) {}
