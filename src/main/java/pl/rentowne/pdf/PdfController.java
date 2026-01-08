package pl.rentowne.pdf;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * Kontroler REST do generowania dokumentów PDF.
 */
@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
class PdfController {

    private final PdfDocumentFactory pdfDocumentFactory;

    // --- Dedykowane rekordy dla tabel ---

    record SampleTableRow(String lp, String name, String value, String status) implements PdfTableRow {
        @Override
        public List<String> toTableRow() {
            return List.of(lp, name, value, status);
        }
    }

    record CurrencyRow(String type, String symbol, String name, String value) implements PdfTableRow {
        @Override
        public List<String> toTableRow() {
            return List.of(type, symbol, name, value);
        }
    }

    record FormRow(String registry, String form, String amount) implements PdfTableRow {
        @Override
        public List<String> toTableRow() {
            return List.of(registry, form, amount);
        }
    }

    record DepositRow(String depositType, String amount) implements PdfTableRow {
        @Override
        public List<String> toTableRow() {
            return List.of(depositType, amount);
        }
    }

    record StringRow(String value) implements PdfTableRow {
        @Override
        public List<String> toTableRow() {
            return List.of(value);
        }
    }

    /**
     * Generuje przykładowy dokument PDF.
     *
     * @param filename opcjonalna nazwa pliku
     * @return dokument PDF
     */
    @GetMapping("/sample")
    @NonNull
    ResponseEntity<ByteArrayResource> generateSamplePdf(
            @RequestParam(defaultValue = "sample-document") final String filename
    ) {
        final PdfDocumentResponse response = pdfDocumentFactory.createDocumentResponse(
                filename,
                builder -> builder
                        .setMetadata(PdfMetadata.builder()
                                .title("Przykładowy dokument PDF")
                                .subject("Dokument demonstracyjny")
                                .keywords("pdf, przykład, demo")
                                .build())
                        .enablePageNumbering()
                        .addPage()
                        .createPrimaryHeader("Przykładowy dokument PDF")
                        .addSpace(20)
                        .createSecondaryHeader("Sekcja 1: Wprowadzenie")
                        .createLeftAlignedParagraph(
                                "Ten dokument został wygenerowany przy użyciu Apache PDFBox. " +
                                "Jest to przykład demonstracyjny pokazujący możliwości API do generowania PDF."
                        )
                        .addSpace(10)
                        .createSecondaryHeader("Sekcja 2: Tabela danych")
                        .createTable(
                                PdfTableConfig.withHeaders(List.of("Lp.", "Nazwa", "Wartość", "Status")),
                                List.of(
                                        new SampleTableRow("1", "Element A", "100.00 PLN", "Aktywny"),
                                        new SampleTableRow("2", "Element B", "250.50 PLN", "Oczekujący"),
                                        new SampleTableRow("3", "Element C", "75.00 PLN", "Zakończony")
                                )
                        )
                        .addSpace(10)
                        .addHorizontalLine()
                        .createCenteredParagraph("Dokument wygenerowany automatycznie")
                        .createRightAlignedParagraph("Data generowania: " + java.time.LocalDate.now())
        );

        return PdfResponseHelper.buildResponse(response);
    }


    /**
     * Generuje dokument inwentaryzacji (Załącznik nr 3a).
     *
     * @return dokument PDF
     */
    @GetMapping("/inventory")
    @NonNull
    ResponseEntity<ByteArrayResource> generateInventoryPdf() throws IOException {
        // Przykładowe dane (w przyszłości z bazy danych)
        final String unitSymbol = "P00252";
        final String unitNameAndAddress = "PARTNER SULEJÓWEK01\nUL. DWORCOWA 50, 05-070 SULEJÓWEK";
        final String inventoryStartDate = "2025-08-18";
        final String inventoryEndDate = "2025-08-22";
        final String printDate = "2025-08-22 08:12";
        final String executedBy = "Anna Testowa 403611";

        // Załaduj logo
        final byte[] logoBytes;
        try (InputStream logoStream = getClass().getResourceAsStream("/images/ibkb-logo.jpg")) {
            logoBytes = Objects.requireNonNull(logoStream).readAllBytes();
        }

        final PdfDocumentResponse response = pdfDocumentFactory.createDocumentResponse(
                "zalacznik-3a",
                builder -> builder
                        .setMetadata(PdfMetadata.builder()
                                .title("Załącznik nr 3a - Inwentaryzacja")
                                .subject("Dokument inwentaryzacji")
                                .build())
                        .addPage()
                        .createTopLeftText("22-08-2025, 08:12")
                        .addLogo(logoBytes, 80f, 40f, TextAlignment.LEFT)
                        .createTopRightText("Załącznik nr 3a")
                        .addSpace(20)
                        .createLabelValueRow("Symbol jednostki:", unitSymbol)
                        .createLabelValueRow("Nazwa i adres jednostki:", unitNameAndAddress)
                        .createLabelValueRow("Data wykonania inwentaryzacji:", inventoryStartDate)
                        .createLabelValueRow("Data zakończenia inwentaryzacji:", inventoryEndDate)
                        .createLabelValueRow("Data wydruku:", printDate)
                        .createLabelValueRow("Wykonał:", executedBy)
                        .addSpace(40)
                        .createTextLine(
                                "PROTOKÓŁ Z INWENTARYZACJI ŚRODKÓW PIENIĘŻNYCH I POZOSTAŁYCH WARTOŚCI",
                                PdfFontConfig.TABLE_HEADER,
                                TextAlignment.CENTER
                        )
                        .createTextLine(
                                "PRZECHOWYWANYCH W SKARBCACH I KASACH JEDNOSTEK PROWADZONYCH W SI BM2",
                                PdfFontConfig.TABLE_HEADER,
                                TextAlignment.CENTER
                        )
                        .createTextLine(
                                "według stanu na dzień " + inventoryStartDate,
                                PdfFontConfig.TABLE_HEADER,
                                TextAlignment.CENTER
                        )
                        .addSpace(40)
                        .createTextLine(
                                "W dniu " + inventoryStartDate + " komisja inwentaryzacyjna przeprowadziła kontrolę kas",
                                PdfFontConfig.DEFAULT,
                                TextAlignment.CENTER
                        )
                        .createTextLine(
                                "oraz ustaliła następujące stany poszczególnych wartości",
                                PdfFontConfig.DEFAULT,
                                TextAlignment.CENTER
                        )
                        .addSpace(20)
                        .createTable(
                                new PdfTableConfig(
                                        4,
                                        new float[]{12f, 18f, 28f, 42f},
                                        List.of("Typ waluty", "Symbol waluty", "Nazwa waluty", "Wartość środków pieniężnych"),
                                        PdfFontConfig.TABLE_HEADER,
                                        PdfFontConfig.DEFAULT
                                ),
                                List.of(
                                        new CurrencyRow("S", "AUD", "Dolar australijski", "0,00"),
                                        new CurrencyRow("S", "CAD", "Dolar kanadyjski", "0,00"),
                                        new CurrencyRow("S", "CHF", "Frank szwajcarski", "0,00"),
                                        new CurrencyRow("S", "CZK", "Korona czeska", "0,00"),
                                        new CurrencyRow("S", "DKK", "Korona duńska", "0,00"),
                                        new CurrencyRow("S", "EUR", "UGW", "0,00"),
                                        new CurrencyRow("S", "GBP", "Funt brytyjski", "0,00"),
                                        new CurrencyRow("S", "HUF", "Forint węgierski", "0,00"),
                                        new CurrencyRow("S", "JPY", "Jen japoński", "0,00"),
                                        new CurrencyRow("S", "NOK", "Korona norweska", "0,00"),
                                        new CurrencyRow("S", "PLN", "Złoty polski", "14 567,56"),
                                        new CurrencyRow("S", "SEK", "Korona szwedzka", "0,00"),
                                        new CurrencyRow("S", "USD", "Dolar amerykański", "0,00")
                                )
                        )
                        // --- Strona 2 ---
                        .addPage()
                        // Tabela: Wg rejestru / Druki ścisłego zarachowania / Stan (szt.)
                        .createTable(
                                new PdfTableConfig(
                                        3,
                                        new float[]{30f, 50f, 20f},
                                        List.of("Wg rejestru", "Druki ścisłego zarachowania", "Stan (szt.)"),
                                        PdfFontConfig.TABLE_HEADER,
                                        PdfFontConfig.DEFAULT
                                ),
                                List.of(
                                        new FormRow("czeki", "czeki", "4"),
                                        new FormRow("Razem", "", "4")
                                )
                        )
                        .addSpace(20)
                        // Tabela: Depozyty / Stan (szt.)
                        .createTable(
                                new PdfTableConfig(
                                        2,
                                        new float[]{85f, 15f},
                                        List.of("Depozyty", "Stan (szt.)"),
                                        PdfFontConfig.TABLE_HEADER,
                                        PdfFontConfig.DEFAULT
                                ),
                                List.of(
                                        new DepositRow("Depozyty rzeczowe zewnętrzne", "1"),
                                        new DepositRow("Depozyty wewnętrzne innych jednostek organizacyjnych Banku (bez umów kredytowych i ich zabezpieczeń)", "2"),
                                        new DepositRow("Kasety i skrytki sejfowe", "1"),
                                        new DepositRow("Inne depozyty przechowywane w kasach/skarbcach (bez umów kredytowych i ich zabezpieczeń)", "3")
                                )
                        )
                        .addSpace(30)
                        // Sekcja: Osoby odpowiedzialne i Komisja inwentaryzacyjna
                        .createNumberedList(
                                "Osoby odpowiedzialne za stan sprawdzanych wartości",
                                List.of(new StringRow("222222"))
                        )
                        .addSpace(20)
                        .createTextLine("Komisja inwentaryzacyjna", PdfFontConfig.TABLE_HEADER, TextAlignment.LEFT)
                        .createLabelValueRow("Przewodniczący:", "1. Anna Testowa 403611")
                        .createLabelValueRow("Członkowie:", "2. Anna Testowa 403613")
                        .addSpace(40)
                        // Tekst końcowy
                        .createTextLine("Stwierdzone stany są zgodne/niezgodne z księgami banku.", PdfFontConfig.DEFAULT, TextAlignment.CENTER)
                        .createTextLine("*niewłaściwe skreślić", PdfFontConfig.DEFAULT, TextAlignment.CENTER)
                        .addSpace(10)
                        .createTextLine("Wykaz ujawnionych różnic i sposób ich rozliczenia (w przypadku wystąpienia różnic sporządzić zestawienie).", PdfFontConfig.DEFAULT, TextAlignment.CENTER)
                        .addSpace(60)
                        // Podpis
                        .createTextLine("........................................................................", PdfFontConfig.DEFAULT, TextAlignment.RIGHT)
                        .createTextLine("Dyrektor Oddziału/Kierujący Jednostką", PdfFontConfig.DEFAULT, TextAlignment.RIGHT)
                        .createTextLine("111111", PdfFontConfig.DEFAULT, TextAlignment.RIGHT)
        );

        return PdfResponseHelper.buildResponse(response);
    }
}
