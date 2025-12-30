package pl.rentowne.settlement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.common.pdf.PdfDocument;
import pl.rentowne.common.pdf.PdfElement;
import pl.rentowne.common.pdf.PdfFacade;

import java.util.List;

/**
 * Kontroler do generowania dokumentów PDF dla rozliczeń.
 * Udostępnia endpoint do generowania przykładowego dokumentu PDF.
 */
@RestController
@RequestMapping("/api/settlements/pdf")
@RequiredArgsConstructor
public final class SettlementPdfController {

    @NonNull
    private final PdfFacade pdfFacade;

    /**
     * Generuje przykładowy dokument PDF z tekstem "Hello World".
     *
     * @return ResponseEntity zawierająca wygenerowany PDF
     */
    @GetMapping("/hello")
    @NonNull
    public ResponseEntity<byte[]> generateHelloWorldPdf() {
        final PdfDocument pdfDocument = PdfDocument.builder()
                .title("Hello World Document")
                .author("Rentowne System")
                .subject("Przykładowy dokument PDF")
                .elements(List.of(
                        PdfElement.title("Hello World"),
                        PdfElement.text("To jest przykładowy dokument PDF wygenerowany przez PdfGeneratorService."),
                        PdfElement.heading("Sekcja demonstracyjna"),
                        PdfElement.text("Tutaj znajduje się tekst standardowy z domyślną czcionką Inter."),
                        PdfElement.boldText("To jest pogrubiony tekst dla podkreślenia ważnych informacji.")
                ))
                .build();

        final byte[] pdfBytes = pdfFacade.generatePdf(pdfDocument);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "hello-world.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}

