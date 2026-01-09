package pl.rentowne.pdf;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Konfiguracja tabeli PDF.
 *
 * @param columnCount   liczba kolumn
 * @param columnWidths  opcjonalne szerokości kolumn (w procentach, suma powinna = 100)
 * @param headers       opcjonalne nagłówki kolumn
 * @param headerConfig  konfiguracja czcionki nagłówków
 * @param cellConfig    konfiguracja czcionki komórek
 */
public record PdfTableConfig(
        int columnCount,
        @Nullable List<Float> columnWidths,
        @Nullable List<String> headers,
        @NonNull PdfFontConfig headerConfig,
        @NonNull PdfFontConfig cellConfig
) {
    /**
     * Tworzy konfigurację tabeli z podaną liczbą kolumn i domyślnymi ustawieniami.
     *
     * @param columnCount liczba kolumn
     * @return konfiguracja tabeli
     */
    public static PdfTableConfig withColumns(int columnCount) {
        return new PdfTableConfig(
                columnCount,
                null,
                null,
                PdfFontConfig.TABLE_HEADER,
                PdfFontConfig.DEFAULT
        );
    }

    /**
     * Tworzy konfigurację tabeli z nagłówkami.
     *
     * @param headers nagłówki kolumn
     * @return konfiguracja tabeli
     */
    public static PdfTableConfig withHeaders(@NonNull List<String> headers) {
        return new PdfTableConfig(
                headers.size(),
                null,
                headers,
                PdfFontConfig.TABLE_HEADER,
                PdfFontConfig.DEFAULT
        );
    }

    /**
     * Zwraca opcjonalne szerokości kolumn.
     *
     * @return szerokości kolumn lub empty
     */
    @NonNull
    public Optional<List<Float>> getColumnWidths() {
        return Optional.ofNullable(columnWidths);
    }

    /**
     * Zwraca opcjonalne nagłówki.
     *
     * @return nagłówki lub empty
     */
    @NonNull
    public Optional<List<String>> getHeaders() {
        return Optional.ofNullable(headers);
    }
}

