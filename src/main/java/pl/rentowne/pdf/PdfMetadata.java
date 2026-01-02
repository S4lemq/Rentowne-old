package pl.rentowne.pdf;

import lombok.Builder;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

/**
 * Metadane dokumentu PDF.
 *
 * @param title        tytuł dokumentu
 * @param subject      temat dokumentu
 * @param keywords     słowa kluczowe (oddzielone przecinkami)
 * @param creationDate data utworzenia
 */
@Builder(toBuilder = true)
public record PdfMetadata(
        @Nullable String title,
        @Nullable String subject,
        @Nullable String keywords,
        @Nullable LocalDateTime creationDate
) {
    /**
     * Domyślny autor dokumentów.
     */
    private static final String DEFAULT_AUTHOR = "Rentowne System";

    /**
     * Domyślna nazwa aplikacji tworzącej dokument.
     */
    private static final String DEFAULT_CREATOR = "Rentowne PDF Generator";

    /**
     * Tworzy instancję z domyślnymi wartościami.
     */
    public PdfMetadata {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }

    /**
     * Zwraca autora dokumentu (zawsze stała wartość).
     *
     * @return autor dokumentu
     */
    public String author() {
        return DEFAULT_AUTHOR;
    }

    /**
     * Zwraca aplikację tworzącą dokument (zawsze stała wartość).
     *
     * @return nazwa aplikacji
     */
    public String creator() {
        return DEFAULT_CREATOR;
    }
}
