package pl.rentowne.rental_document;

import java.util.UUID;

/** Lightweight document state displayed alongside its financial source. */
public record DocumentSummary(UUID id, String number, String status, boolean hasPdf, String mailState) {}
