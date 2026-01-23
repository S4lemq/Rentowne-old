package pl.rentowne.pdf;

import org.springframework.lang.NonNull;

/**
 * Odpowiedź zawierająca wygenerowany dokument PDF.
 *
 * @param content  zawartość PDF jako tablica bajtów
 * @param filename nazwa pliku
 */
@SuppressWarnings("java:S6218") // byte[] używane ze względu na wydajność
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
     * Zwraca referencję do wewnętrznej tablicy bajtów bez klonowania.
     * <p>
     * <strong>UWAGA:</strong> Metoda package-private dla wydajności.
     * Używać tylko w zaufanych klasach pakietu, które nie modyfikują tablicy.
     * </p>
     *
     * @return referencja do wewnętrznej tablicy bajtów (NIE MODYFIKOWAĆ!)
     */
    @NonNull
    byte[] contentUnsafe() {
        return content;
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

