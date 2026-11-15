package pl.rentowne.rental_document;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DocumentHistory(
        UUID id,
        String kind,
        String state,
        String to,
        List<String> cc,
        OffsetDateTime createdAt,
        OffsetDateTime finishedAt,
        String error,
        Long requestedBy) {}
