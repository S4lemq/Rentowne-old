package pl.rentowne.invoice;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/providers")
public class InvoiceComparisonController {

    private final PdfTextExtractor pdfTextExtractor;
    private final InvoiceParser invoiceParser;
    private final ProviderInvoiceComparator comparator;

    public InvoiceComparisonController(
            PdfTextExtractor pdfTextExtractor,
            InvoiceParser invoiceParser,
            ProviderInvoiceComparator comparator) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.invoiceParser = invoiceParser;
        this.comparator = comparator;
    }

    @PostMapping(path = "/{id}/invoice/compare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ComparisonResponse compare(
            @PathVariable("id") Long providerId,
            @RequestPart("file") MultipartFile pdf) {

        String text = pdfTextExtractor.extractText(pdf);
        var lines = invoiceParser.parseElectricityInvoice(text);
        return comparator.compare(providerId, lines);
    }
}

