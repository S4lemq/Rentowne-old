package pl.rentowne.pdf;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Kontroler REST do generowania dokumentów PDF.
 */
@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
class PdfController {

    private final PdfDocumentFactory pdfDocumentFactory;

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
                                .author("Rentowne System")
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
                                        List.of("1", "Element A", "100.00 PLN", "Aktywny"),
                                        List.of("2", "Element B", "250.50 PLN", "Oczekujący"),
                                        List.of("3", "Element C", "75.00 PLN", "Zakończony")
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
     * Generuje dokument PDF i wyświetla go w przeglądarce (inline).
     *
     * @param filename opcjonalna nazwa pliku
     * @return dokument PDF do wyświetlenia inline
     */
    @GetMapping("/preview")
    @NonNull
    ResponseEntity<ByteArrayResource> previewPdf(
            @RequestParam(defaultValue = "preview-document") final String filename
    ) {
        final PdfDocumentResponse response = pdfDocumentFactory.createDocumentResponse(
                filename,
                builder -> builder
                        .addPage()
                        .createPrimaryHeader("Podgląd dokumentu")
                        .createLeftAlignedParagraph("Ten dokument jest wyświetlany bezpośrednio w przeglądarce.")
        );

        return PdfResponseHelper.buildResponse(response, true);
    }
}
