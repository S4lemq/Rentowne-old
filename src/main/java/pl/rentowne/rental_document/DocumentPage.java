package pl.rentowne.rental_document;

import java.util.List;

public record DocumentPage(List<DocumentItem> content, long totalElements) {}
