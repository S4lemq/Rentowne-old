package pl.rentowne.pdf;

import lombok.Builder;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

/**
 * Metadane dokumentu PDF.
 *
 * @param title        tytuł dokumentu
 * @param author       autor dokumentu
 * @param subject      temat dokumentu
 * @param keywords     słowa kluczowe (oddzielone przecinkami)
 * @param creator      aplikacja tworząca dokument
 * @param creationDate data utworzenia
 */
@Builder(toBuilder = true)
public record PdfMetadata(
        @Nullable String title,
        @Nullable String author,
        @Nullable String subject,
        @Nullable String keywords,
        @Nullable String creator,
        @Nullable LocalDateTime creationDate
) {
    /**
     * Domyślna nazwa aplikacji tworzącej dokument.
     */
    private static final String DEFAULT_CREATOR = "Rentowne PDF Generator";

    /**
     * Tworzy instancję z domyślnymi wartościami.
     */
    public PdfMetadata {
        if (creator == null) {
            creator = DEFAULT_CREATOR;
        }
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }
}
