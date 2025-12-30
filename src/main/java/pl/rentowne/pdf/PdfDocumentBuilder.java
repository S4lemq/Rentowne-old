package pl.rentowne.pdf;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import pl.rentowne.pdf.exception.PdfErrorCode;
import pl.rentowne.pdf.exception.PdfGenerationException;

import java.io.IOException;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

/**
 * Builder dokumentu PDF - główna klasa do budowania dokumentów.
 * Udostępnia fluent API do tworzenia zawartości PDF.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class PdfDocumentBuilder implements AutoCloseable {

    private final PDDocument document;
    private final PdfTextHelper textHelper;

    @Getter(AccessLevel.PACKAGE)
    private final PdfMargins margins;

    @Getter(AccessLevel.PACKAGE)
    private final PDRectangle pageSize;

    @Nullable
    private PDPage currentPage;

    @Nullable
    private PDPageContentStream currentContentStream;

    private float currentY;

    @NonNull
    private PageNumberingConfig pageNumberingConfig = PageNumberingConfig.DISABLED;

    @Nullable
    private PdfMetadata metadata;

    /**
     * Włącza numerowanie stron z domyślną konfiguracją (wycentrowane).
     *
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder enablePageNumbering() {
        return enablePageNumbering(PageNumberingConfig.CENTERED);
    }

    /**
     * Włącza numerowanie stron z podaną konfiguracją.
     *
     * @param config konfiguracja numerowania stron
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder enablePageNumbering(@NonNull final PageNumberingConfig config) {
        this.pageNumberingConfig = config;
        return this;
    }

    /**
     * Ustawia metadane dokumentu PDF.
     *
     * @param metadata metadane dokumentu
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder setMetadata(@NonNull final PdfMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Dodaje nową stronę do dokumentu.
     *
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder addPage() {
        closeCurrentContentStream();

        currentPage = new PDPage(pageSize);
        document.addPage(currentPage);

        try {
            currentContentStream = new PDPageContentStream(document, currentPage);
            currentY = pageSize.getHeight() - margins.top();
        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.PAGE_CREATION_ERROR, e);
        }

        return this;
    }

    /**
     * Tworzy główny nagłówek (wycentrowany, duża pogrubiona czcionka).
     *
     * @param text tekst nagłówka
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createPrimaryHeader(@NonNull final String text) {
        return createParagraph(text, PdfFontConfig.PRIMARY_HEADER, TextAlignment.CENTER);
    }

    /**
     * Tworzy drugorzędny nagłówek (wycentrowany, średnia pogrubiona czcionka).
     *
     * @param text tekst nagłówka
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createSecondaryHeader(@NonNull final String text) {
        return createParagraph(text, PdfFontConfig.SECONDARY_HEADER, TextAlignment.CENTER);
    }

    /**
     * Tworzy paragraf wyrównany do lewej.
     *
     * @param text tekst paragrafu
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createLeftAlignedParagraph(@NonNull final String text) {
        return createParagraph(text, PdfFontConfig.DEFAULT, TextAlignment.LEFT);
    }

    /**
     * Tworzy paragraf wyrównany do prawej.
     *
     * @param text tekst paragrafu
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createRightAlignedParagraph(@NonNull final String text) {
        return createParagraph(text, PdfFontConfig.DEFAULT, TextAlignment.RIGHT);
    }

    /**
     * Tworzy paragraf wycentrowany.
     *
     * @param text tekst paragrafu
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createCenteredParagraph(@NonNull final String text) {
        return createParagraph(text, PdfFontConfig.DEFAULT, TextAlignment.CENTER);
    }

    /**
     * Tworzy paragraf z pełną konfiguracją.
     *
     * @param text       tekst paragrafu
     * @param fontConfig konfiguracja czcionki
     * @param alignment  wyrównanie tekstu
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createParagraph(
            @NonNull final String text,
            @NonNull final PdfFontConfig fontConfig,
            @NonNull final TextAlignment alignment
    ) {
        ensurePageExists();

        try {
            final float contentWidth = pageSize.getWidth() - margins.left() - margins.right();
            final List<String> lines = textHelper.wrapText(text, contentWidth, fontConfig);
            final float lineHeight = textHelper.calculateLineHeight(fontConfig);

            for (final String line : lines) {
                checkPageBreak(lineHeight);
                final PDPageContentStream stream = getContentStream();

                final float x = textHelper.calculateXPosition(line, fontConfig, alignment, pageSize, margins);
                textHelper.writeText(stream, line, x, currentY, fontConfig);
                currentY -= lineHeight;
            }

            // Dodaj odstęp po paragrafie
            currentY -= lineHeight * 0.5f;

        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.PARAGRAPH_CREATION_ERROR, e);
        }

        return this;
    }

    /**
     * Tworzy tabelę na podstawie konfiguracji i danych.
     *
     * @param config konfiguracja tabeli
     * @param rows   wiersze danych (każdy wiersz to lista wartości komórek)
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createTable(@NonNull final PdfTableConfig config, @NonNull final List<List<String>> rows) {
        ensurePageExists();

        final float tableWidth = pageSize.getWidth() - margins.left() - margins.right();
        final float[] columnWidths = calculateColumnWidths(config, tableWidth);

        // Dodaj nagłówki jeśli są
        config.getHeaders().ifPresent(headers -> addTableRow(headers, columnWidths, config.headerConfig()));

        // Dodaj wiersze danych
        for (final List<String> row : rows) {
            addTableRow(row, columnWidths, config.cellConfig());
        }

        // Odstęp po tabeli
        currentY -= textHelper.calculateLineHeight(config.cellConfig());

        return this;
    }

    /**
     * Dodaje nagłówki do tabeli (metoda pomocnicza do ręcznego tworzenia tabel).
     *
     * @param headers      nagłówki
     * @param columnWidths szerokości kolumn
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder addTableHeaders(@NonNull final List<String> headers, @NonNull final float[] columnWidths) {
        addTableRow(headers, columnWidths, PdfFontConfig.TABLE_HEADER);
        return this;
    }

    /**
     * Dodaje logo/obraz do dokumentu.
     *
     * @param imageBytes bajty obrazu
     * @param width      szerokość obrazu (musi być większa od 0)
     * @param height     wysokość obrazu (musi być większa od 0)
     * @param alignment  wyrównanie obrazu
     * @return ten builder
     * @throws PdfGenerationException gdy wymiary są nieprawidłowe lub bajty obrazu są puste
     */
    @NonNull
    public PdfDocumentBuilder addLogo(
            @NonNull final byte[] imageBytes,
            final float width,
            final float height,
            @NonNull final TextAlignment alignment
    ) {
        if (width <= 0 || height <= 0) {
            throw PdfGenerationException.errorWithExtDescription(PdfErrorCode.INVALID_IMAGE_DIMENSIONS, width, height);
        }
        if (imageBytes.length == 0) {
            throw PdfGenerationException.error(PdfErrorCode.EMPTY_IMAGE_BYTES);
        }

        ensurePageExists();

        try {
            final PDImageXObject image = PDImageXObject.createFromByteArray(document, imageBytes, "logo");

            checkPageBreak(height);
            final PDPageContentStream stream = getContentStream();
            final float x = calculateImageXPosition(width, alignment);

            stream.drawImage(image, x, currentY - height, width, height);
            currentY -= height + 10; // Dodaj odstęp po obrazie

        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.LOGO_ADDITION_ERROR, e);
        }

        return this;
    }

    /**
     * Dodaje pusty odstęp pionowy.
     *
     * @param space wysokość odstępu w punktach (musi być większa od 0)
     * @return ten builder
     * @throws PdfGenerationException gdy space jest mniejsze lub równe 0
     */
    @NonNull
    public PdfDocumentBuilder addSpace(final float space) {
        if (space <= 0) {
            throw PdfGenerationException.errorWithExtDescription(PdfErrorCode.INVALID_SPACE_VALUE, space);
        }
        checkPageBreak(space);
        currentY -= space;
        return this;
    }

    /**
     * Dodaje linię poziomą.
     *
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder addHorizontalLine() {
        ensurePageExists();

        final PDPageContentStream stream = getContentStream();

        try {
            final float startX = margins.left();
            final float endX = pageSize.getWidth() - margins.right();

            stream.moveTo(startX, currentY);
            stream.lineTo(endX, currentY);
            stream.stroke();

            currentY -= 10;
        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.HORIZONTAL_LINE_ERROR, e);
        }

        return this;
    }

    /**
     * Buduje dokument PDF i zwraca go jako tablicę bajtów.
     *
     * @return bajty dokumentu PDF
     */
    @NonNull
    byte[] build() {
        closeCurrentContentStream();
        applyMetadata();
        applyPageNumbering();

        try (final var outputStream = new java.io.ByteArrayOutputStream()) {
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.DOCUMENT_SAVE_ERROR, e);
        }
    }

    @Override
    public void close() {
        closeCurrentContentStream();
        try {
            document.close();
        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.DOCUMENT_CLOSE_ERROR, e);
        }
    }

    private void applyMetadata() {
        if (metadata == null) {
            return;
        }

        final PDDocumentInformation info = document.getDocumentInformation();

        Optional.ofNullable(metadata.title()).ifPresent(info::setTitle);
        Optional.ofNullable(metadata.author()).ifPresent(info::setAuthor);
        Optional.ofNullable(metadata.subject()).ifPresent(info::setSubject);
        Optional.ofNullable(metadata.keywords()).ifPresent(info::setKeywords);
        Optional.ofNullable(metadata.creator()).ifPresent(info::setCreator);
        Optional.ofNullable(metadata.creationDate())
                .map(date -> GregorianCalendar.from(date.atZone(ZoneId.systemDefault())))
                .ifPresent(info::setCreationDate);
    }

    private void applyPageNumbering() {
        if (!pageNumberingConfig.enabled()) {
            return;
        }

        final int totalPages = document.getNumberOfPages();

        for (int i = 0; i < totalPages; i++) {
            final PDPage page = document.getPage(i);
            final String pageNumberText = pageNumberingConfig.formatPageNumber(i + 1, totalPages);

            try (final PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                final float x = textHelper.calculateXPosition(
                        pageNumberText,
                        pageNumberingConfig.fontConfig(),
                        pageNumberingConfig.alignment(),
                        pageSize,
                        margins
                );
                final float y = pageNumberingConfig.marginFromBottom();

                textHelper.writeText(contentStream, pageNumberText, x, y, pageNumberingConfig.fontConfig());

            } catch (final IOException e) {
                throw PdfGenerationException.error(PdfErrorCode.PAGE_NUMBERING_ERROR, e);
            }
        }
    }

    @NonNull
    private PDPageContentStream getContentStream() {
        if (currentContentStream == null) {
            throw PdfGenerationException.error(PdfErrorCode.CONTENT_STREAM_ERROR);
        }
        return currentContentStream;
    }

    private void ensurePageExists() {
        if (currentPage == null) {
            addPage();
        }
    }

    private void closeCurrentContentStream() {
        if (currentContentStream != null) {
            try {
                currentContentStream.close();
                currentContentStream = null;
            } catch (final IOException e) {
                throw PdfGenerationException.error(PdfErrorCode.CONTENT_STREAM_CLOSE_ERROR, e);
            }
        }
    }

    private void checkPageBreak(final float requiredSpace) {
        if (currentY - requiredSpace < margins.bottom()) {
            addPage();
        }
    }

    private float[] calculateColumnWidths(@NonNull final PdfTableConfig config, final float tableWidth) {
        return config.getColumnWidths()
                .map(widths -> {
                    final float[] absoluteWidths = new float[widths.length];
                    for (int i = 0; i < widths.length; i++) {
                        absoluteWidths[i] = tableWidth * widths[i] / 100f;
                    }
                    return absoluteWidths;
                })
                .orElseGet(() -> {
                    final float[] equalWidths = new float[config.columnCount()];
                    final float width = tableWidth / config.columnCount();
                    for (int i = 0; i < config.columnCount(); i++) {
                        equalWidths[i] = width;
                    }
                    return equalWidths;
                });
    }

    private void addTableRow(
            @NonNull final List<String> cells,
            @NonNull final float[] columnWidths,
            @NonNull final PdfFontConfig fontConfig
    ) {
        final float lineHeight = textHelper.calculateLineHeight(fontConfig);
        final float rowHeight = lineHeight * 1.5f;

        checkPageBreak(rowHeight);

        final PDPageContentStream stream = getContentStream();

        try {
            float x = margins.left();

            // Rysuj ramki komórek i tekst
            for (int i = 0; i < cells.size() && i < columnWidths.length; i++) {
                final float cellWidth = columnWidths[i];
                final String cellText = cells.get(i) != null ? cells.get(i) : "";

                // Ramka komórki
                stream.addRect(x, currentY - rowHeight, cellWidth, rowHeight);
                stream.stroke();

                // Tekst komórki (wycentrowany w komórce)
                final float textWidth = textHelper.calculateTextWidth(cellText, fontConfig);
                final float textX = x + (cellWidth - textWidth) / 2;
                final float textY = currentY - rowHeight + (rowHeight - lineHeight) / 2 + lineHeight * 0.25f;

                textHelper.writeText(stream, cellText, textX, textY, fontConfig);

                x += cellWidth;
            }

            currentY -= rowHeight;

        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.TABLE_ROW_CREATION_ERROR, e);
        }
    }

    private float calculateImageXPosition(final float imageWidth, @NonNull final TextAlignment alignment) {
        final float contentWidth = pageSize.getWidth() - margins.left() - margins.right();

        return switch (alignment) {
            case LEFT -> margins.left();
            case CENTER -> margins.left() + (contentWidth - imageWidth) / 2;
            case RIGHT -> pageSize.getWidth() - margins.right() - imageWidth;
        };
    }
}

