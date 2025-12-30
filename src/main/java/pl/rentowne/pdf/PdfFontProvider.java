package pl.rentowne.pdf;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import pl.rentowne.pdf.exception.PdfErrorCode;
import pl.rentowne.pdf.exception.PdfGenerationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Zarządza ładowaniem i dostarczaniem czcionek dla dokumentów PDF.
 */
@Component
@RequiredArgsConstructor
class PdfFontProvider {

    private static final String FONTS_PATH = "fonts/";
    private static final Map<FontStyle, String> FONT_FILES = Map.of(
            FontStyle.REGULAR, "Inter-Regular.ttf",
            FontStyle.BOLD, "Inter-Bold.ttf",
            FontStyle.ITALIC, "Inter-Italic.ttf",
            FontStyle.BOLD_ITALIC, "Inter-BoldItalic.ttf"
    );

    /**
     * Ładuje wszystkie czcionki dla dokumentu.
     *
     * @param document dokument PDF
     * @return mapa styl czcionki -> załadowana czcionka
     * @throws PdfGenerationException gdy nie można załadować czcionek
     */
    @NonNull
    Map<FontStyle, PDFont> loadFonts(@NonNull final PDDocument document) {
        final Map<FontStyle, PDFont> fonts = new EnumMap<>(FontStyle.class);

        for (final Map.Entry<FontStyle, String> entry : FONT_FILES.entrySet()) {
            try {
                final PDFont font = loadFont(document, entry.getValue());
                fonts.put(entry.getKey(), font);
            } catch (final IOException e) {
                throw PdfGenerationException.error(PdfErrorCode.FONT_LOADING_ERROR, e, entry.getValue());
            }
        }

        return fonts;
    }

    /**
     * Ładuje pojedynczą czcionkę.
     *
     * @param document dokument PDF
     * @param fontFile nazwa pliku czcionki
     * @return załadowana czcionka
     * @throws IOException gdy nie można załadować pliku
     */
    @NonNull
    private PDFont loadFont(@NonNull final PDDocument document, @NonNull final String fontFile) throws IOException {
        final ClassPathResource resource = new ClassPathResource(FONTS_PATH + fontFile);
        try (final InputStream is = resource.getInputStream()) {
            return PDType0Font.load(document, is);
        }
    }
}
