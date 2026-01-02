package pl.rentowne.pdf;

/**
 * Marginesy strony PDF.
 *
 * @param top    margines górny w punktach
 * @param right  margines prawy w punktach
 * @param bottom margines dolny w punktach
 * @param left   margines lewy w punktach
 */
public record PdfMargins(
        float top,
        float right,
        float bottom,
        float left
) {
    /**
     * Konstruktor kanoniczny z walidacją.
     *
     * @throws IllegalArgumentException gdy którykolwiek margines jest ujemny
     */
    public PdfMargins {
        if (top < 0 || right < 0 || bottom < 0 || left < 0) {
            throw new IllegalArgumentException(
                    String.format("Marginesy nie mogą być ujemne: top=%.2f, right=%.2f, bottom=%.2f, left=%.2f",
                            top, right, bottom, left)
            );
        }
    }

    /**
     * Domyślne marginesy (50 punktów z każdej strony).
     */
    public static final PdfMargins DEFAULT = new PdfMargins(50f, 50f, 50f, 50f);

    /**
     * Wąskie marginesy (25 punktów).
     */
    public static final PdfMargins NARROW = new PdfMargins(25f, 25f, 25f, 25f);

    /**
     * Szerokie marginesy (72 punkty = 1 cal).
     */
    public static final PdfMargins WIDE = new PdfMargins(72f, 72f, 72f, 72f);

    /**
     * Tworzy marginesy o równych wartościach.
     *
     * @param margin wartość marginesu
     * @return marginesy
     */
    public static PdfMargins uniform(float margin) {
        return new PdfMargins(margin, margin, margin, margin);
    }
}

