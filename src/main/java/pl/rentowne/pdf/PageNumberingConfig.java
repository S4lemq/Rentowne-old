package pl.rentowne.pdf;

import org.springframework.lang.NonNull;

/**
 * Konfiguracja numerowania stron.
 *
 * @param enabled          czy numerowanie jest włączone
 * @param alignment        wyrównanie numeru strony
 * @param format           format numeru strony (np. "Strona %d z %d")
 * @param fontConfig       konfiguracja czcionki
 * @param marginFromBottom margines od dolnej krawędzi strony
 */
public record PageNumberingConfig(
        boolean enabled,
        @NonNull TextAlignment alignment,
        @NonNull String format,
        @NonNull PdfFontConfig fontConfig,
        float marginFromBottom
) {
    /**
     * Domyślna konfiguracja - wyłączona.
     */
    public static final PageNumberingConfig DISABLED = new PageNumberingConfig(
            false,
            TextAlignment.CENTER,
            "Strona %d z %d",
            PdfFontConfig.SMALL,
            25f
    );

    /**
     * Numerowanie wycentrowane na dole strony.
     */
    public static final PageNumberingConfig CENTERED = new PageNumberingConfig(
            true,
            TextAlignment.CENTER,
            "Strona %d z %d",
            PdfFontConfig.SMALL,
            25f
    );

    /**
     * Numerowanie po prawej stronie na dole.
     */
    public static final PageNumberingConfig RIGHT_ALIGNED = new PageNumberingConfig(
            true,
            TextAlignment.RIGHT,
            "%d / %d",
            PdfFontConfig.SMALL,
            25f
    );

    /**
     * Numerowanie po lewej stronie na dole.
     */
    public static final PageNumberingConfig LEFT_ALIGNED = new PageNumberingConfig(
            true,
            TextAlignment.LEFT,
            "Strona %d",
            PdfFontConfig.SMALL,
            25f
    );

    /**
     * Tworzy konfigurację z własnym formatem.
     *
     * @param format    format numeru strony (%d = numer bieżący, drugi %d = całkowita liczba)
     * @param alignment wyrównanie
     * @return konfiguracja numerowania
     */
    public static PageNumberingConfig withFormat(@NonNull String format, @NonNull TextAlignment alignment) {
        return new PageNumberingConfig(true, alignment, format, PdfFontConfig.SMALL, 25f);
    }

    /**
     * Formatuje numer strony.
     *
     * @param currentPage numer bieżącej strony
     * @param totalPages  całkowita liczba stron
     * @return sformatowany tekst
     */
    @NonNull
    String formatPageNumber(int currentPage, int totalPages) {
        if (format.contains("%d") && format.indexOf("%d") != format.lastIndexOf("%d")) {
            return String.format(format, currentPage, totalPages);
        }
        return String.format(format, currentPage);
    }
}

