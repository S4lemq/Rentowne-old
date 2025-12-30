package pl.rentowne.pdf;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.lang.NonNull;
import pl.rentowne.pdf.exception.PdfErrorCode;
import pl.rentowne.pdf.exception.PdfGenerationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pomocnik do operacji tekstowych w PDF.
 * Odpowiada za obliczanie wymiarów tekstu i zawijanie linii.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class PdfTextHelper {

    private final Map<FontStyle, PDFont> fonts;

    /**
     * Oblicza szerokość tekstu dla podanej konfiguracji czcionki.
     *
     * @param text       tekst do zmierzenia
     * @param fontConfig konfiguracja czcionki
     * @return szerokość tekstu w punktach
     */
    float calculateTextWidth(@NonNull final String text, @NonNull final PdfFontConfig fontConfig) {
        try {
            final PDFont font = fonts.get(fontConfig.fontStyle());
            return font.getStringWidth(text) / 1000f * fontConfig.fontSize().getSize();
        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.TEXT_WIDTH_CALCULATION_ERROR, e);
        }
    }

    /**
     * Oblicza wysokość linii dla podanej konfiguracji czcionki.
     *
     * @param fontConfig konfiguracja czcionki
     * @return wysokość linii w punktach
     */
    float calculateLineHeight(@NonNull final PdfFontConfig fontConfig) {
        return fontConfig.fontSize().getSize() * 1.2f;
    }

    /**
     * Dzieli tekst na linie mieszczące się w podanej szerokości.
     *
     * @param text       tekst do podzielenia
     * @param maxWidth   maksymalna szerokość linii
     * @param fontConfig konfiguracja czcionki
     * @return lista linii tekstu
     */
    @NonNull
    List<String> wrapText(@NonNull final String text, final float maxWidth, @NonNull final PdfFontConfig fontConfig) {
        if (text.isBlank()) {
            return List.of();
        }

        final List<String> lines = new ArrayList<>();
        final String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (final String word : words) {
            final String testLine = currentLine.isEmpty()
                    ? word
                    : currentLine + " " + word;

            if (calculateTextWidth(testLine, fontConfig) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * Oblicza pozycję X tekstu w zależności od wyrównania.
     *
     * @param text       tekst
     * @param fontConfig konfiguracja czcionki
     * @param alignment  wyrównanie
     * @param pageSize   rozmiar strony
     * @param margins    marginesy
     * @return pozycja X
     */
    float calculateXPosition(
            @NonNull final String text,
            @NonNull final PdfFontConfig fontConfig,
            @NonNull final TextAlignment alignment,
            @NonNull final PDRectangle pageSize,
            @NonNull final PdfMargins margins
    ) {
        final float contentWidth = pageSize.getWidth() - margins.left() - margins.right();
        final float textWidth = calculateTextWidth(text, fontConfig);

        return switch (alignment) {
            case LEFT -> margins.left();
            case CENTER -> margins.left() + (contentWidth - textWidth) / 2;
            case RIGHT -> pageSize.getWidth() - margins.right() - textWidth;
        };
    }

    /**
     * Pisze tekst na strumień treści PDF.
     *
     * @param contentStream strumień treści
     * @param text          tekst do napisania
     * @param x             pozycja X
     * @param y             pozycja Y
     * @param fontConfig    konfiguracja czcionki
     * @throws IOException gdy wystąpi błąd I/O
     */
    void writeText(
            @NonNull final PDPageContentStream contentStream,
            @NonNull final String text,
            final float x,
            final float y,
            @NonNull final PdfFontConfig fontConfig
    ) throws IOException {
        final PDFont font = fonts.get(fontConfig.fontStyle());
        contentStream.beginText();
        contentStream.setFont(font, fontConfig.fontSize().getSize());
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }
}
