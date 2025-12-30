package pl.rentowne.common.pdf;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Publiczna fasada do generowania dokumentów PDF.
 * Stanowi jedyny punkt wejścia do funkcjonalności generowania PDF.
 * Enkapsuluje całą logikę wewnętrzną pakietu pdf.
 */
@Component
@RequiredArgsConstructor
public final class PdfFacade {

    @NonNull
    private final PdfGeneratorService pdfGeneratorService;

    /**
     * Generuje dokument PDF na podstawie obiektu PdfDocument.
     *
     * @param pdfDocument model dokumentu zawierający dane do wygenerowania
     * @return tablica bajtów reprezentująca wygenerowany PDF
     * @throws PdfGenerationException gdy wystąpi błąd podczas generowania
     */
    @NonNull
    public byte[] generatePdf(@NonNull final PdfDocument pdfDocument) {
        return pdfGeneratorService.generatePdf(pdfDocument);
    }
}

