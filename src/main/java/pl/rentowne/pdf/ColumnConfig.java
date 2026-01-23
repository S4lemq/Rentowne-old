package pl.rentowne.pdf;

import org.springframework.lang.NonNull;

/**
 * Konfiguracja pojedynczej kolumny tabeli PDF.
 *
 * @param header    nagłówek kolumny
 * @param width     szerokość kolumny (w procentach)
 * @param alignment wyrównanie tekstu w komórkach (nie dotyczy nagłówka - nagłówki są zawsze wycentrowane)
 */
public record ColumnConfig(
        @NonNull String header,
        float width,
        @NonNull TextAlignment alignment
) {
    /**
     * Tworzy konfigurację kolumny z podanym nagłówkiem, szerokością i domyślnym wyrównaniem (CENTER).
     *
     * @param header nagłówek kolumny
     * @param width  szerokość kolumny w procentach
     * @return konfiguracja kolumny
     */
    public static ColumnConfig of(@NonNull String header, float width) {
        return new ColumnConfig(header, width, TextAlignment.CENTER);
    }

    /**
     * Tworzy konfigurację kolumny z podanym nagłówkiem, szerokością i wyrównaniem.
     *
     * @param header    nagłówek kolumny
     * @param width     szerokość kolumny w procentach
     * @param alignment wyrównanie tekstu w komórkach
     * @return konfiguracja kolumny
     */
    public static ColumnConfig of(@NonNull String header, float width, @NonNull TextAlignment alignment) {
        return new ColumnConfig(header, width, alignment);
    }
}
