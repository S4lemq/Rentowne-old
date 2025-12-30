package pl.rentowne.common.pdf;

import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.NonNull;

/**
 * Reprezentuje pojedynczy element zawartości dokumentu PDF.
 * Element może być tekstem, nagłówkiem, tytułem itp.
 */
@Getter
@Builder
public final class PdfElement {

    @NonNull
    private final PdfElementType type;

    @NonNull
    private final String content;

    @Builder.Default
    private final float fontSize = 0f; // 0 oznacza użycie domyślnego rozmiaru dla typu

    @Builder.Default
    private final boolean bold = false;

    /**
     * Typ elementu PDF określający sposób jego renderowania.
     */
    public enum PdfElementType {
        /** Tytuł dokumentu */
        TITLE,
        /** Nagłówek sekcji */
        HEADING,
        /** Standardowy tekst */
        TEXT,
        /** Tekst pogrubiony */
        BOLD_TEXT
    }

    /**
     * Pomocnicza metoda do tworzenia elementu tytułu.
     *
     * @param content treść tytułu
     * @return element PDF typu TITLE
     */
    @NonNull
    public static PdfElement title(@NonNull final String content) {
        return PdfElement.builder()
                .type(PdfElementType.TITLE)
                .content(content)
                .bold(true)
                .build();
    }

    /**
     * Pomocnicza metoda do tworzenia elementu nagłówka.
     *
     * @param content treść nagłówka
     * @return element PDF typu HEADING
     */
    @NonNull
    public static PdfElement heading(@NonNull final String content) {
        return PdfElement.builder()
                .type(PdfElementType.HEADING)
                .content(content)
                .bold(true)
                .build();
    }

    /**
     * Pomocnicza metoda do tworzenia elementu tekstowego.
     *
     * @param content treść tekstu
     * @return element PDF typu TEXT
     */
    @NonNull
    public static PdfElement text(@NonNull final String content) {
        return PdfElement.builder()
                .type(PdfElementType.TEXT)
                .content(content)
                .build();
    }

    /**
     * Pomocnicza metoda do tworzenia pogrubionego elementu tekstowego.
     *
     * @param content treść tekstu
     * @return element PDF typu BOLD_TEXT
     */
    @NonNull
    public static PdfElement boldText(@NonNull final String content) {
        return PdfElement.builder()
                .type(PdfElementType.BOLD_TEXT)
                .content(content)
                .bold(true)
                .build();
    }
}

