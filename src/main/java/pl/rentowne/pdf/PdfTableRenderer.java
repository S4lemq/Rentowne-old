package pl.rentowne.pdf;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.lang.NonNull;
import pl.rentowne.pdf.exception.PdfErrorCode;
import pl.rentowne.pdf.exception.PdfGenerationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Renderer tabel PDF.
 * Odpowiada za rysowanie tabel w dokumentach PDF.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class PdfTableRenderer {

    private static final float TABLE_CELL_PADDING = 3f;
    private static final float CELL_TEXT_Y_OFFSET = 0.75f;

    private final PdfTextHelper textHelper;

    /**
     * Kontekst renderowania tabeli - przekazuje informacje o stanie dokumentu.
     */
    record RenderContext(
            float tableWidth,
            float marginLeft,
            Supplier<Float> currentYSupplier,
            Consumer<Float> currentYConsumer,
            Supplier<PDPageContentStream> contentStreamSupplier,
            Consumer<Float> pageBreakChecker
    ) {
        float getCurrentY() {
            return currentYSupplier.get();
        }

        void setCurrentY(float y) {
            currentYConsumer.accept(y);
        }

        PDPageContentStream getContentStream() {
            return contentStreamSupplier.get();
        }

        void checkPageBreak(float requiredSpace) {
            pageBreakChecker.accept(requiredSpace);
        }
    }

    /**
     * Renderuje tabelę na podstawie konfiguracji i danych.
     *
     * @param config  konfiguracja tabeli
     * @param rows    wiersze danych (każdy wiersz to lista wartości komórek)
     * @param context kontekst renderowania
     */
    void renderTable(
            @NonNull final PdfTableConfig config,
            @NonNull final List<List<String>> rows,
            @NonNull final RenderContext context
    ) {
        final float[] columnWidths = calculateColumnWidths(config, context.tableWidth());

        // Dodaj nagłówki jeśli są
        config.getHeaders().ifPresent(headers ->
                renderRow(headers, columnWidths, config.headerConfig(), context)
        );

        // Dodaj wiersze danych
        for (final List<String> row : rows) {
            renderRow(row, columnWidths, config.cellConfig(), context);
        }

        // Odstęp po tabeli
        context.setCurrentY(context.getCurrentY() - textHelper.calculateLineHeight(config.cellConfig()));
    }

    /**
     * Renderuje nagłówki tabeli.
     *
     * @param headers      nagłówki
     * @param columnWidths szerokości kolumn
     * @param context      kontekst renderowania
     */
    void renderHeaders(
            @NonNull final List<String> headers,
            @NonNull final float[] columnWidths,
            @NonNull final RenderContext context
    ) {
        renderRow(headers, columnWidths, PdfFontConfig.TABLE_HEADER, context);
    }

    /**
     * Oblicza szerokości kolumn na podstawie konfiguracji.
     *
     * @param config     konfiguracja tabeli
     * @param tableWidth szerokość tabeli
     * @return tablica z szerokościami kolumn
     */
    float[] calculateColumnWidths(@NonNull final PdfTableConfig config, final float tableWidth) {
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

    /**
     * Renderuje pojedynczy wiersz tabeli.
     *
     * @param cells        komórki wiersza
     * @param columnWidths szerokości kolumn
     * @param fontConfig   konfiguracja czcionki
     * @param context      kontekst renderowania
     */
    private void renderRow(
            @NonNull final List<String> cells,
            @NonNull final float[] columnWidths,
            @NonNull final PdfFontConfig fontConfig,
            @NonNull final RenderContext context
    ) {
        final float lineHeight = textHelper.calculateLineHeight(fontConfig);

        // Oblicz liczbę linii dla każdej komórki i znajdź maksymalną
        final List<List<String>> wrappedCells = new ArrayList<>();
        int maxLines = 1;

        for (int i = 0; i < cells.size() && i < columnWidths.length; i++) {
            final String cellText = cells.get(i) != null ? cells.get(i) : "";
            final float maxTextWidth = columnWidths[i] - (2 * TABLE_CELL_PADDING);
            final List<String> lines = textHelper.wrapText(cellText, maxTextWidth, fontConfig);
            wrappedCells.add(lines.isEmpty() ? List.of("") : lines);
            maxLines = Math.max(maxLines, lines.size());
        }

        final float rowHeight = lineHeight * maxLines + (TABLE_CELL_PADDING * 2);

        context.checkPageBreak(rowHeight);

        final PDPageContentStream stream = context.getContentStream();

        try {
            float x = context.marginLeft();
            final float currentY = context.getCurrentY();

            // Rysuj ramki komórek i tekst
            for (int i = 0; i < wrappedCells.size() && i < columnWidths.length; i++) {
                final float cellWidth = columnWidths[i];
                final List<String> lines = wrappedCells.get(i);

                // Ramka komórki
                stream.addRect(x, currentY - rowHeight, cellWidth, rowHeight);
                stream.stroke();

                // Rysuj każdą linię tekstu
                float textY = currentY - TABLE_CELL_PADDING - lineHeight * CELL_TEXT_Y_OFFSET;
                for (final String line : lines) {
                    final float textWidth = textHelper.calculateTextWidth(line, fontConfig);

                    // Pierwsza kolumna wyrównana do lewej, pozostałe wycentrowane
                    final float textX;
                    if (i == 0) {
                        textX = x + TABLE_CELL_PADDING;
                    } else {
                        textX = x + (cellWidth - textWidth) / 2;
                    }

                    textHelper.writeText(stream, line, textX, textY, fontConfig);
                    textY -= lineHeight;
                }

                x += cellWidth;
            }

            context.setCurrentY(currentY - rowHeight);

        } catch (final IOException e) {
            throw PdfGenerationException.error(PdfErrorCode.TABLE_ROW_CREATION_ERROR, e);
        }
    }
}

