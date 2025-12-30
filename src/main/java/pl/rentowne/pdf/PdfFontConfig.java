package pl.rentowne.pdf;

import org.springframework.lang.NonNull;

/**
 * Konfiguracja czcionki dla tekstu w PDF.
 *
 * @param fontSize  rozmiar czcionki
 * @param fontStyle styl czcionki
 */
public record PdfFontConfig(
        @NonNull FontSize fontSize,
        @NonNull FontStyle fontStyle
) {
    /**
     * Domyślna konfiguracja czcionki (normalny rozmiar, zwykły styl).
     */
    public static final PdfFontConfig DEFAULT = new PdfFontConfig(FontSize.NORMAL, FontStyle.REGULAR);

    /**
     * Konfiguracja dla nagłówka głównego.
     */
    public static final PdfFontConfig PRIMARY_HEADER = new PdfFontConfig(FontSize.LARGE, FontStyle.BOLD);

    /**
     * Konfiguracja dla nagłówka drugorzędnego.
     */
    public static final PdfFontConfig SECONDARY_HEADER = new PdfFontConfig(FontSize.MEDIUM, FontStyle.BOLD);

    /**
     * Konfiguracja dla nagłówków tabeli.
     */
    public static final PdfFontConfig TABLE_HEADER = new PdfFontConfig(FontSize.NORMAL, FontStyle.BOLD);

    /**
     * Konfiguracja dla małego tekstu (np. numerowanie stron, przypisy).
     */
    public static final PdfFontConfig SMALL = new PdfFontConfig(FontSize.SMALL, FontStyle.REGULAR);
}

