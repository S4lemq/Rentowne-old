package pl.rentowne.common.pdf;

import lombok.Getter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Konfiguracja parametrów do generowania dokumentów PDF.
 * Definiuje domyślne czcionki, rozmiary i inne właściwości wykorzystywane przez generator PDF.
 */
@Configuration
@Getter
class PdfConfiguration {

    private static final String DEFAULT_FONT_PATH = "/fonts/Inter-Regular.ttf";
    private static final String DEFAULT_FONT_BOLD_PATH = "/fonts/Inter-Bold.ttf";
    private static final float DEFAULT_FONT_SIZE = 12f;
    private static final float DEFAULT_TITLE_FONT_SIZE = 18f;
    private static final float DEFAULT_HEADING_FONT_SIZE = 14f;

    private final float defaultFontSize;
    private final float titleFontSize;
    private final float headingFontSize;

    PdfConfiguration() {
        this.defaultFontSize = DEFAULT_FONT_SIZE;
        this.titleFontSize = DEFAULT_TITLE_FONT_SIZE;
        this.headingFontSize = DEFAULT_HEADING_FONT_SIZE;
    }

    /**
     * Ładuje domyślną czcionkę Inter dla dokumentu PDF.
     *
     * @param document dokument PDF, dla którego ładowana jest czcionka
     * @return załadowana czcionka PDFont
     * @throws IOException gdy nie można załadować pliku czcionki
     */
    @NonNull
    PDFont loadDefaultFont(@NonNull final PDDocument document) throws IOException {
        try (final InputStream fontStream = getClass().getResourceAsStream(DEFAULT_FONT_PATH)) {
            if (fontStream == null) {
                throw new IOException("Nie można znaleźć czcionki: " + DEFAULT_FONT_PATH);
            }
            return PDType0Font.load(document, fontStream);
        }
    }

    /**
     * Ładuje pogrubioną czcionkę Inter dla dokumentu PDF.
     *
     * @param document dokument PDF, dla którego ładowana jest czcionka
     * @return załadowana czcionka PDFont
     * @throws IOException gdy nie można załadować pliku czcionki
     */
    @NonNull
    PDFont loadBoldFont(@NonNull final PDDocument document) throws IOException {
        try (final InputStream fontStream = getClass().getResourceAsStream(DEFAULT_FONT_BOLD_PATH)) {
            if (fontStream == null) {
                throw new IOException("Nie można znaleźć czcionki: " + DEFAULT_FONT_BOLD_PATH);
            }
            return PDType0Font.load(document, fontStream);
        }
    }
}

