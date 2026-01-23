package pl.rentowne.pdf;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
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

    private static final float LABEL_WIDTH_RATIO = 0.35f;
    private static final float ROW_SPACING_FACTOR = 0.3f;
    private static final float TABLE_CELL_PADDING = 3f;
    private static final float CELL_TEXT_Y_OFFSET = 0.75f;
    private static final float IMAGE_BOTTOM_MARGIN = 10f;
    private static final float HORIZONTAL_LINE_MARGIN = 10f;
    private static final float PARAGRAPH_SPACING_FACTOR = 0.5f;

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

    @Nullable
    private PdfEncryptionConfig encryptionConfig;

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
     * Włącza szyfrowanie dokumentu PDF z podaną konfiguracją.
     *
     * @param config konfiguracja szyfrowania
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder setEncryption(@NonNull final PdfEncryptionConfig config) {
        this.encryptionConfig = config;
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
     * Tworzy tekst w lewym górnym rogu strony (np. dla daty).
     *
     * @param text tekst do wyświetlenia
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createTopLeftText(@NonNull final String text) {
        return createTopLeftText(text, PdfFontConfig.DEFAULT);
    }

    /**
     * Tworzy tekst w lewym górnym rogu strony z niestandardową konfiguracją czcionki.
     *
     * @param text       tekst do wyświetlenia
     * @param fontConfig konfiguracja czcionki
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createTopLeftText(@NonNull final String text, @NonNull final PdfFontConfig fontConfig) {
        ensurePageExists();

        try {
            final float lineHeight = textHelper.calculateLineHeight(fontConfig);
            checkPageBreak(lineHeight);

            final PDPageContentStream stream = getContentStream();
            final float x = margins.left();

            textHelper.writeText(stream, text, x, currentY, fontConfig);
            currentY -= lineHeight;

        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.PARAGRAPH_CREATION_ERROR, e);
        }

        return this;
    }

    /**
     * Tworzy tekst w prawym górnym rogu strony (np. dla załączników).
     *
     * @param text tekst do wyświetlenia
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createTopRightText(@NonNull final String text) {
        return createTopRightText(text, PdfFontConfig.DEFAULT);
    }

    /**
     * Tworzy tekst w prawym górnym rogu strony z niestandardową konfiguracją czcionki.
     *
     * @param text       tekst do wyświetlenia
     * @param fontConfig konfiguracja czcionki
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createTopRightText(@NonNull final String text, @NonNull final PdfFontConfig fontConfig) {
        ensurePageExists();

        try {
            final PDPageContentStream stream = getContentStream();
            final float textWidth = textHelper.calculateTextWidth(text, fontConfig);
            final float x = pageSize.getWidth() - margins.right() - textWidth;
            final float y = pageSize.getHeight() - margins.top();

            textHelper.writeText(stream, text, x, y, fontConfig);

        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.PARAGRAPH_CREATION_ERROR, e);
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
     * Tworzy linię tekstu z automatycznym zawijaniem gdy się nie mieści.
     *
     * @param text       tekst do wyświetlenia
     * @param fontConfig konfiguracja czcionki
     * @param alignment  wyrównanie tekstu
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createTextLine(
            @NonNull final String text,
            @NonNull final PdfFontConfig fontConfig,
            @NonNull final TextAlignment alignment
    ) {
        ensurePageExists();

        try {
            final float lineHeight = textHelper.calculateLineHeight(fontConfig);
            final float contentWidth = pageSize.getWidth() - margins.left() - margins.right();

            final List<String> lines = textHelper.wrapText(text, contentWidth, fontConfig);
            for (final String line : lines) {
                checkPageBreak(lineHeight);
                final PDPageContentStream stream = getContentStream();
                final float x = textHelper.calculateXPosition(line, fontConfig, alignment, pageSize, margins);
                textHelper.writeText(stream, line, x, currentY, fontConfig);
                currentY -= lineHeight;
            }

        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.PARAGRAPH_CREATION_ERROR, e);
        }

        return this;
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
            currentY -= lineHeight * PARAGRAPH_SPACING_FACTOR;

        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.PARAGRAPH_CREATION_ERROR, e);
        }

        return this;
    }

    /**
     * Układ listy numerowanej z etykietą.
     */
    public enum ListLayout {
        /**
         * Lista w linii - etykieta i pierwszy element w tej samej linii.
         * <pre>
         * Członkowie: 1. Anna Testowa
         *             2. Adam Nowak
         *             3. Szymon Kowal
         * </pre>
         */
        INLINE,

        /**
         * Lista blokowa - lista pod etykietą.
         * <pre>
         * Członkowie:
         * 1. Anna Testowa
         * 2. Adam Nowak
         * 3. Szymon Kowal
         * </pre>
         */
        BLOCK
    }

    /**
     * Tworzy listę numerowaną z etykietą (domyślnie układ INLINE).
     *
     * @param label etykieta wyświetlana przed listą
     * @param rows  elementy listy implementujące {@link PdfTableRow}
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createLabeledNumberedList(
            @NonNull final String label,
            @NonNull final List<? extends PdfTableRow> rows
    ) {
        return createLabeledNumberedList(label, rows, ListLayout.INLINE, PdfFontConfig.DEFAULT, PdfFontConfig.TABLE_HEADER);
    }

    /**
     * Tworzy listę numerowaną z etykietą i określonym układem.
     *
     * @param label  etykieta wyświetlana przed listą
     * @param rows   elementy listy implementujące {@link PdfTableRow}
     * @param layout układ listy (INLINE lub BLOCK)
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createLabeledNumberedList(
            @NonNull final String label,
            @NonNull final List<? extends PdfTableRow> rows,
            @NonNull final ListLayout layout
    ) {
        return createLabeledNumberedList(label, rows, layout, PdfFontConfig.DEFAULT, PdfFontConfig.TABLE_HEADER);
    }

    /**
     * Tworzy listę numerowaną z etykietą i niestandardowymi konfiguracjami czcionek (domyślnie układ INLINE).
     *
     * @param label       etykieta wyświetlana przed listą
     * @param rows        elementy listy implementujące {@link PdfTableRow}
     * @param labelConfig konfiguracja czcionki dla etykiety
     * @param itemConfig  konfiguracja czcionki dla elementów listy
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createLabeledNumberedList(
            @NonNull final String label,
            @NonNull final List<? extends PdfTableRow> rows,
            @NonNull final PdfFontConfig labelConfig,
            @NonNull final PdfFontConfig itemConfig
    ) {
        return createLabeledNumberedList(label, rows, ListLayout.INLINE, labelConfig, itemConfig);
    }

    /**
     * Tworzy listę numerowaną z etykietą, określonym układem i niestandardowymi konfiguracjami czcionek.
     *
     * @param label       etykieta wyświetlana przed listą
     * @param rows        elementy listy implementujące {@link PdfTableRow}
     * @param layout      układ listy (INLINE lub BLOCK)
     * @param labelConfig konfiguracja czcionki dla etykiety
     * @param itemConfig  konfiguracja czcionki dla elementów listy
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createLabeledNumberedList(
            @NonNull final String label,
            @NonNull final List<? extends PdfTableRow> rows,
            @NonNull final ListLayout layout,
            @NonNull final PdfFontConfig labelConfig,
            @NonNull final PdfFontConfig itemConfig
    ) {
        if (rows.isEmpty()) {
            return this;
        }

        ensurePageExists();

        try {
            final float lineHeight = Math.max(
                    textHelper.calculateLineHeight(labelConfig),
                    textHelper.calculateLineHeight(itemConfig)
            );
            final float contentWidth = pageSize.getWidth() - margins.left() - margins.right();
            final float valueX = margins.left() + (contentWidth * LABEL_WIDTH_RATIO);

            if (layout == ListLayout.INLINE) {
                // INLINE: etykieta i pierwszy element w tej samej linii
                checkPageBreak(lineHeight);
                final PDPageContentStream stream = getContentStream();

                // Etykieta
                textHelper.writeText(stream, label, margins.left(), currentY, labelConfig);

                // Pierwszy element obok etykiety
                final String firstItem = "1. " + String.join(" ", rows.get(0).toTableRow());
                textHelper.writeText(stream, firstItem, valueX, currentY, itemConfig);
                currentY -= lineHeight;

                // Pozostałe elementy wyrównane do pierwszego
                for (int i = 1; i < rows.size(); i++) {
                    checkPageBreak(lineHeight);
                    final String numberedItem = (i + 1) + ". " + String.join(" ", rows.get(i).toTableRow());
                    textHelper.writeText(getContentStream(), numberedItem, valueX, currentY, itemConfig);
                    currentY -= lineHeight;
                }
            } else {
                // BLOCK: lista pod etykietą
                checkPageBreak(lineHeight);
                final PDPageContentStream stream = getContentStream();

                // Etykieta
                textHelper.writeText(stream, label, margins.left(), currentY, labelConfig);
                currentY -= lineHeight;

                // Wszystkie elementy pod etykietą
                for (int i = 0; i < rows.size(); i++) {
                    checkPageBreak(lineHeight);
                    final String numberedItem = (i + 1) + ". " + String.join(" ", rows.get(i).toTableRow());
                    textHelper.writeText(getContentStream(), numberedItem, margins.left(), currentY, itemConfig);
                    currentY -= lineHeight;
                }
            }

            // Odstęp po liście
            currentY -= lineHeight * ROW_SPACING_FACTOR;

        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.PARAGRAPH_CREATION_ERROR, e);
        }

        return this;
    }

    /**
     * Tworzy tabelę na podstawie konfiguracji i danych obiektów implementujących {@link PdfTableRow}.
     * Ta metoda automatycznie konwertuje obiekty na wiersze tabeli używając {@link PdfTableRow#toTableRow()}.
     *
     * @param config konfiguracja tabeli
     * @param rows   lista obiektów implementujących {@link PdfTableRow}
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createTable(@NonNull final PdfTableConfig config,
                                          @NonNull final List<? extends PdfTableRow> rows) {
        ensurePageExists();

        final float tableWidth = pageSize.getWidth() - margins.left() - margins.right();
        final float[] columnWidths = calculateColumnWidths(config, tableWidth);
        final TextAlignment[] columnAlignments = calculateColumnAlignments(config);
        final TextAlignment[] headerAlignments = createCenteredAlignments(config.columnCount());

        // Dodaj nagłówki (nagłówki zawsze wycentrowane)
        addTableRow(config.getHeaders(), columnWidths, config.headerConfig(), headerAlignments);

        // Dodaj wiersze danych z wyrównaniem kolumn
        for (final PdfTableRow row : rows) {
            addTableRow(row.toTableRow(), columnWidths, config.cellConfig(), columnAlignments);
        }

        // Odstęp po tabeli
        currentY -= textHelper.calculateLineHeight(config.cellConfig());

        return this;
    }

    /**
     * Dodaje nagłówki do tabeli (metoda pomocnicza do ręcznego tworzenia tabel).
     * Nagłówki są zawsze wycentrowane.
     *
     * @param headers      nagłówki
     * @param columnWidths szerokości kolumn
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder addTableHeaders(@NonNull final List<String> headers, @NonNull final float[] columnWidths) {
        addTableRow(headers, columnWidths, PdfFontConfig.TABLE_HEADER, createCenteredAlignments(headers.size()));
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
            currentY -= height + IMAGE_BOTTOM_MARGIN;

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
     * Tworzy wiersz z etykietą i wartością (etykieta normalna, wartość pogrubiona).
     *
     * @param label etykieta (wyświetlana normalną czcionką)
     * @param value wartość (wyświetlana pogrubioną czcionką)
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createLabelValueRow(@NonNull final String label, @NonNull final String value) {
        return createLabelValueRow(label, value, PdfFontConfig.DEFAULT, PdfFontConfig.TABLE_HEADER);
    }

    /**
     * Tworzy wiersz z etykietą i wartością z niestandardowymi konfiguracjami czcionek.
     * Obsługuje wartości wieloliniowe (rozdzielone przez \n) oraz automatyczne zawijanie tekstu.
     *
     * @param label       etykieta
     * @param value       wartość (może zawierać \n dla wielu linii)
     * @param labelConfig konfiguracja czcionki dla etykiety
     * @param valueConfig konfiguracja czcionki dla wartości
     * @return ten builder
     */
    @NonNull
    public PdfDocumentBuilder createLabelValueRow(
            @NonNull final String label,
            @NonNull final String value,
            @NonNull final PdfFontConfig labelConfig,
            @NonNull final PdfFontConfig valueConfig
    ) {
        ensurePageExists();

        try {
            final float lineHeight = Math.max(
                    textHelper.calculateLineHeight(labelConfig),
                    textHelper.calculateLineHeight(valueConfig)
            );

            // Oblicz pozycję X dla wartości
            final float contentWidth = pageSize.getWidth() - margins.left() - margins.right();
            final float valueX = margins.left() + (contentWidth * LABEL_WIDTH_RATIO);
            final float valueMaxWidth = contentWidth - (contentWidth * LABEL_WIDTH_RATIO);

            // Podziel wartość na linie (ręczne podziały przez \n + automatyczne zawijanie)
            final String[] manualLines = value.split("\n");
            final List<String> allValueLines = new java.util.ArrayList<>();

            for (final String manualLine : manualLines) {
                final List<String> wrappedLines = textHelper.wrapText(manualLine, valueMaxWidth, valueConfig);
                if (wrappedLines.isEmpty()) {
                    allValueLines.add("");
                } else {
                    allValueLines.addAll(wrappedLines);
                }
            }

            final float totalHeight = lineHeight * allValueLines.size();

            checkPageBreak(totalHeight);
            final PDPageContentStream stream = getContentStream();

            // Etykieta po lewej (tylko w pierwszej linii)
            textHelper.writeText(stream, label, margins.left(), currentY, labelConfig);

            // Wartość z przesunięciem proporcjonalnym do szerokości strony (może być wieloliniowa)
            float valueY = currentY;

            for (final String line : allValueLines) {
                textHelper.writeText(stream, line, valueX, valueY, valueConfig);
                valueY -= lineHeight;
            }

            currentY -= totalHeight + (lineHeight * ROW_SPACING_FACTOR);

        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.PARAGRAPH_CREATION_ERROR, e);
        }

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

            currentY -= HORIZONTAL_LINE_MARGIN;
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
        applyEncryption();

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

    private void applyEncryption() {
        if (encryptionConfig == null) {
            return;
        }

        try {
            final AccessPermission permissions = getAccessPermission();

            final StandardProtectionPolicy protectionPolicy = new StandardProtectionPolicy(
                    encryptionConfig.ownerPassword(),
                    encryptionConfig.userPassword(),
                    permissions
            );
            protectionPolicy.setEncryptionKeyLength(encryptionConfig.keyLength());

            document.protect(protectionPolicy);
        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.ENCRYPTION_ERROR, e);
        }
    }

    private AccessPermission getAccessPermission() {
        final AccessPermission permissions = new AccessPermission();
        final PdfEncryptionConfig.PdfPermissions perms = encryptionConfig.permissions();

        permissions.setCanPrint(perms.canPrint());
        permissions.setCanModify(perms.canModify());
        permissions.setCanExtractContent(perms.canExtractContent());
        permissions.setCanModifyAnnotations(perms.canModifyAnnotations());
        permissions.setCanFillInForm(perms.canFillInForm());
        permissions.setCanExtractForAccessibility(perms.canExtractForAccessibility());
        permissions.setCanAssembleDocument(perms.canAssembleDocument());
        return permissions;
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
        final List<Float> widths = config.getWidths();
        final float[] absoluteWidths = new float[widths.size()];
        for (int i = 0; i < widths.size(); i++) {
            absoluteWidths[i] = tableWidth * widths.get(i) / 100f;
        }
        return absoluteWidths;
    }

    @NonNull
    private TextAlignment[] calculateColumnAlignments(@NonNull final PdfTableConfig config) {
        final List<TextAlignment> alignments = config.getAlignments();
        return alignments.toArray(new TextAlignment[0]);
    }

    @NonNull
    private TextAlignment[] createCenteredAlignments(final int columnCount) {
        final TextAlignment[] alignments = new TextAlignment[columnCount];
        java.util.Arrays.fill(alignments, TextAlignment.CENTER);
        return alignments;
    }

    private void addTableRow(
            @NonNull final List<String> cells,
            @NonNull final float[] columnWidths,
            @NonNull final PdfFontConfig fontConfig,
            @Nullable final TextAlignment[] columnAlignments
    ) {
        final float lineHeight = textHelper.calculateLineHeight(fontConfig);
        final float cellPadding = TABLE_CELL_PADDING;

        // Oblicz liczbę linii dla każdej komórki i znajdź maksymalną
        final List<List<String>> wrappedCells = new java.util.ArrayList<>();
        int maxLines = 1;

        for (int i = 0; i < cells.size() && i < columnWidths.length; i++) {
            final String cellText = cells.get(i) != null ? cells.get(i) : "";
            final float maxTextWidth = columnWidths[i] - (2 * cellPadding);
            final List<String> lines = textHelper.wrapText(cellText, maxTextWidth, fontConfig);
            wrappedCells.add(lines.isEmpty() ? List.of("") : lines);
            maxLines = Math.max(maxLines, lines.size());
        }

        final float rowHeight = lineHeight * maxLines + (cellPadding * 2);

        checkPageBreak(rowHeight);

        final PDPageContentStream stream = getContentStream();

        try {
            float x = margins.left();

            // Rysuj ramki komórek i tekst
            for (int i = 0; i < wrappedCells.size() && i < columnWidths.length; i++) {
                final float cellWidth = columnWidths[i];
                final List<String> lines = wrappedCells.get(i);

                // Ramka komórki
                stream.addRect(x, currentY - rowHeight, cellWidth, rowHeight);
                stream.stroke();

                // Określ wyrównanie dla tej kolumny
                final TextAlignment alignment = getColumnAlignment(i, columnAlignments);

                // Rysuj każdą linię tekstu
                float textY = currentY - cellPadding - lineHeight * CELL_TEXT_Y_OFFSET;
                for (final String line : lines) {
                    final float textWidth = textHelper.calculateTextWidth(line, fontConfig);
                    final float textX = calculateCellTextX(x, cellWidth, cellPadding, textWidth, alignment);

                    textHelper.writeText(stream, line, textX, textY, fontConfig);
                    textY -= lineHeight;
                }

                x += cellWidth;
            }

            currentY -= rowHeight;

        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.TABLE_ROW_CREATION_ERROR, e);
        }
    }

    @NonNull
    private TextAlignment getColumnAlignment(final int columnIndex, @Nullable final TextAlignment[] columnAlignments) {
        // Jeśli brak konfiguracji wyrównania, domyślnie: pierwsza kolumna LEFT, pozostałe CENTER
        if (columnAlignments == null || columnIndex >= columnAlignments.length) {
            return columnIndex == 0 ? TextAlignment.LEFT : TextAlignment.CENTER;
        }
        return columnAlignments[columnIndex];
    }

    private float calculateCellTextX(
            final float cellX,
            final float cellWidth,
            final float cellPadding,
            final float textWidth,
            @NonNull final TextAlignment alignment
    ) {
        return switch (alignment) {
            case LEFT -> cellX + cellPadding;
            case CENTER -> cellX + (cellWidth - textWidth) / 2;
            case RIGHT -> cellX + cellWidth - cellPadding - textWidth;
        };
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

