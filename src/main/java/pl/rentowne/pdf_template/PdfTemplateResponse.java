package pl.rentowne.pdf_template;

import org.springframework.lang.NonNull;

/**
 * Odpowiedź zawierająca wygenerowany dokument PDF z szablonu HTML.
 *
 * @param content  zawartość PDF jako tablica bajtów
 * @param filename nazwa pliku
 */
public record PdfTemplateResponse(
        @NonNull byte[] content,
        @NonNull String filename
) {
    /**
     * Zwraca rozmiar dokumentu w bajtach.
     *
     * @return rozmiar w bajtach
     */
    public int size() {
        return content.length;
    }
}

