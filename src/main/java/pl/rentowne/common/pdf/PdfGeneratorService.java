package pl.rentowne.common.pdf;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Serwis odpowiedzialny za generowanie dokumentów PDF.
 * Zapewnia uniwersalne metody do tworzenia dokumentów PDF z różnych źródeł danych.
 */
@Service
@RequiredArgsConstructor
final class PdfGeneratorService {

    @NonNull
    private final PdfConfiguration pdfConfiguration;

    private static final float MARGIN = 50f;
    private static final float LINE_SPACING = 1.5f;

    /**
     * Generuje dokument PDF na podstawie obiektu PdfDocument.
     *
     * @param pdfDocument model dokumentu zawierający dane do wygenerowania
     * @return tablica bajtów reprezentująca wygenerowany PDF
     * @throws PdfGenerationException gdy wystąpi błąd podczas generowania
     */
    @NonNull
    byte[] generatePdf(@NonNull final PdfDocument pdfDocument) {
        try (final PDDocument document = new PDDocument();
             final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            setDocumentMetadata(document, pdfDocument);

            final PDFont defaultFont = pdfConfiguration.loadDefaultFont(document);
            final PDFont boldFont = pdfConfiguration.loadBoldFont(document);

            renderContent(document, pdfDocument, defaultFont, boldFont);

            document.save(outputStream);
            return outputStream.toByteArray();

        } catch (final IOException e) {
            throw new PdfGenerationException("Błąd podczas generowania dokumentu PDF", e);
        }
    }

    /**
     * Ustawia metadane dokumentu PDF.
     *
     * @param document dokument PDF
     * @param pdfDocument model zawierający metadane
     */
    private void setDocumentMetadata(
            @NonNull final PDDocument document,
            @NonNull final PdfDocument pdfDocument
    ) {
        final var info = document.getDocumentInformation();
        info.setTitle(pdfDocument.getTitle());
        pdfDocument.getAuthor().ifPresent(info::setAuthor);
        pdfDocument.getSubject().ifPresent(info::setSubject);
    }

    /**
     * Renderuje zawartość dokumentu PDF.
     *
     * @param document dokument PDF
     * @param pdfDocument model zawierający elementy do wyrenderowania
     * @param defaultFont domyślna czcionka
     * @param boldFont czcionka pogrubiona
     * @throws IOException gdy wystąpi błąd zapisu
     */
    private void renderContent(
            @NonNull final PDDocument document,
            @NonNull final PdfDocument pdfDocument,
            @NonNull final PDFont defaultFont,
            @NonNull final PDFont boldFont
    ) throws IOException {
        if (pdfDocument.getElements().isEmpty()) {
            return;
        }

        PDPage page = createNewPage(document);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        float currentY = page.getMediaBox().getHeight() - MARGIN;

        for (final PdfElement element : pdfDocument.getElements()) {
            final float fontSize = determineFontSize(element);
            final PDFont font = element.isBold() ? boldFont : defaultFont;
            final float lineHeight = fontSize * LINE_SPACING;

            // Sprawdź czy potrzebna jest nowa strona
            if (currentY - lineHeight < MARGIN) {
                contentStream.close();
                page = createNewPage(document);
                contentStream = new PDPageContentStream(document, page);
                currentY = page.getMediaBox().getHeight() - MARGIN;
            }

            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(MARGIN, currentY);
            contentStream.showText(element.getContent());
            contentStream.endText();

            currentY -= lineHeight;

            // Dodaj dodatkową przestrzeń po tytułach i nagłówkach
            if (element.getType() == PdfElement.PdfElementType.TITLE ||
                element.getType() == PdfElement.PdfElementType.HEADING) {
                currentY -= fontSize * 0.5f;
            }
        }

        contentStream.close();
    }

    /**
     * Określa rozmiar czcionki dla elementu PDF.
     *
     * @param element element PDF
     * @return rozmiar czcionki
     */
    private float determineFontSize(@NonNull final PdfElement element) {
        if (element.getFontSize() > 0) {
            return element.getFontSize();
        }

        return switch (element.getType()) {
            case TITLE -> pdfConfiguration.getTitleFontSize();
            case HEADING -> pdfConfiguration.getHeadingFontSize();
            case TEXT, BOLD_TEXT -> pdfConfiguration.getDefaultFontSize();
        };
    }

    /**
     * Tworzy nową stronę PDF.
     *
     * @param document dokument PDF
     * @return nowa strona
     */
    @NonNull
    private PDPage createNewPage(@NonNull final PDDocument document) {
        final PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return page;
    }
}

