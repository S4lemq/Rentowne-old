package pl.rentowne.pdf;

import org.springframework.lang.NonNull;

import java.util.List;

/**
 * Konfiguracja tabeli PDF.
 *
 * @param columns      konfiguracja kolumn (nagłówki, szerokości w procentach - suma powinna = 100, oraz wyrównanie)
 * @param headerConfig konfiguracja czcionki nagłówków
 * @param cellConfig   konfiguracja czcionki komórek
 */
public record PdfTableConfig(
        @NonNull List<ColumnConfig> columns,
        @NonNull PdfFontConfig headerConfig,
        @NonNull PdfFontConfig cellConfig
) {
    /**
     * Tworzy konfigurację tabeli z kolumnami i domyślnymi ustawieniami czcionek.
     *
     * @param columns konfiguracja kolumn
     * @return konfiguracja tabeli
     */
    public static PdfTableConfig withColumns(@NonNull List<ColumnConfig> columns) {
        return new PdfTableConfig(
                columns,
                PdfFontConfig.TABLE_HEADER,
                PdfFontConfig.DEFAULT
        );
    }

    /**
     * Zwraca liczbę kolumn.
     *
     * @return liczba kolumn
     */
    public int columnCount() {
        return columns.size();
    }

    /**
     * Zwraca nagłówki wyekstrahowane z konfiguracji kolumn.
     *
     * @return lista nagłówków
     */
    @NonNull
    public List<String> getHeaders() {
        return columns.stream()
                .map(ColumnConfig::header)
                .toList();
    }

    /**
     * Zwraca szerokości kolumn wyekstrahowane z konfiguracji kolumn.
     *
     * @return lista szerokości (w procentach)
     */
    @NonNull
    public List<Float> getWidths() {
        return columns.stream()
                .map(ColumnConfig::width)
                .toList();
    }

    /**
     * Zwraca wyrównania kolumn wyekstrahowane z konfiguracji kolumn.
     *
     * @return lista wyrównań
     */
    @NonNull
    public List<TextAlignment> getAlignments() {
        return columns.stream()
                .map(ColumnConfig::alignment)
                .toList();
    }
}
