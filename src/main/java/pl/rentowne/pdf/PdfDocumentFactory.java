package pl.rentowne.pdf;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import pl.rentowne.pdf.exception.PdfErrorCode;
import pl.rentowne.pdf.exception.PdfGenerationException;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Fabryka do tworzenia dokumentów PDF.
 * Główny punkt wejścia do generowania dokumentów.
 */
@Service
@RequiredArgsConstructor
public class PdfDocumentFactory {

    private final PdfFontProvider fontProvider;

    /**
     * Tworzy nowy dokument PDF z domyślnymi ustawieniami (A4, standardowe marginesy).
     *
     * @param builderConsumer funkcja budująca zawartość dokumentu
     * @return bajty wygenerowanego dokumentu
     */
    @NonNull
    public byte[] createDocument(@NonNull final Consumer<PdfDocumentBuilder> builderConsumer) {
        return createDocument(PDRectangle.A4, PdfMargins.DEFAULT, builderConsumer);
    }

    /**
     * Tworzy nowy dokument PDF z niestandardowymi ustawieniami.
     *
     * @param pageSize        rozmiar strony
     * @param margins         marginesy
     * @param builderConsumer funkcja budująca zawartość dokumentu
     * @return bajty wygenerowanego dokumentu
     */
    @NonNull
    public byte[] createDocument(
            @NonNull final PDRectangle pageSize,
            @NonNull final PdfMargins margins,
            @NonNull final Consumer<PdfDocumentBuilder> builderConsumer
    ) {
        try (final PDDocument document = new PDDocument()) {
            final Map<FontStyle, PDFont> fonts = fontProvider.loadFonts(document);
            final PdfTextHelper textHelper = new PdfTextHelper(fonts);
            final PdfTableRenderer tableRenderer = new PdfTableRenderer(textHelper);

            try (final PdfDocumentBuilder builder = new PdfDocumentBuilder(document, textHelper, tableRenderer, margins, pageSize)) {
                builderConsumer.accept(builder);
                return builder.build();
            }
        } catch (final Exception e) {
            if (e instanceof PdfGenerationException pdfEx) {
                throw pdfEx;
            }
            throw PdfGenerationException.error(PdfErrorCode.DOCUMENT_GENERATION_ERROR, e);
        }
    }

    /**
     * Tworzy odpowiedź z dokumentem PDF.
     *
     * @param filename        nazwa pliku
     * @param builderConsumer funkcja budująca zawartość dokumentu
     * @return odpowiedź z dokumentem PDF
     */
    @NonNull
    public PdfDocumentResponse createDocumentResponse(
            @NonNull final String filename,
            @NonNull final Consumer<PdfDocumentBuilder> builderConsumer
    ) {
        final byte[] content = createDocument(builderConsumer);
        return new PdfDocumentResponse(content, ensurePdfExtension(filename));
    }

    @NonNull
    private String ensurePdfExtension(@NonNull final String filename) {
        return filename.toLowerCase().endsWith(".pdf") ? filename : filename + ".pdf";
    }
}
