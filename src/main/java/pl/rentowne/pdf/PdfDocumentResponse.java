package pl.rentowne.pdf;

import org.springframework.lang.NonNull;

/**
 * Odpowiedź zawierająca wygenerowany dokument PDF.
 *
 * @param content  zawartość PDF jako tablica bajtów
 * @param filename nazwa pliku
 */
public record PdfDocumentResponse(
        @NonNull byte[] content,
        @NonNull String filename
) {
    /**
     * Konstruktor kanoniczny z defensywną kopią tablicy bajtów.
     *
     * @param content  zawartość PDF jako tablica bajtów
     * @param filename nazwa pliku
     */
    public PdfDocumentResponse {
        content = content.clone();
    }

    /**
     * Zwraca kopię zawartości dokumentu PDF.
     * Kopia defensywna zapobiega modyfikacji wewnętrznego stanu.
     *
     * @return kopia zawartości PDF
     */
    @Override
    public byte[] content() {
        return content.clone();
    }

    /**
     * Zwraca rozmiar dokumentu w bajtach.
     *
     * @return rozmiar w bajtach
     */
    public int size() {
        return content.length;
    }
}

