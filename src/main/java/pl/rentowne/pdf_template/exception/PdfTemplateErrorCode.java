package pl.rentowne.pdf_template.exception;

/**
 * Kody błędów generowania PDF z szablonów.
 */
public enum PdfTemplateErrorCode {
    TEMPLATE_NOT_FOUND("Nie znaleziono szablonu PDF"),
    TEMPLATE_PROCESSING_ERROR("Błąd przetwarzania szablonu"),
    PDF_GENERATION_ERROR("Błąd generowania dokumentu PDF"),
    FONT_LOADING_ERROR("Błąd ładowania czcionki");

    private final String message;

    PdfTemplateErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

