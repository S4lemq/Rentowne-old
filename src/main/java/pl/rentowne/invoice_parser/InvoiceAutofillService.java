package pl.rentowne.invoice_parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.rentowne.housing_provider.model.ProviderType;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceAutofillService {

    private final PdfTextExtractor pdfTextExtractor;
    private final InvoiceParser invoiceParser;


    public HousingProviderAutofillResponse autofillFromPdf(MultipartFile pdf, ProviderType type) {
        if (type != ProviderType.ELECTRICITY) {
            throw new IllegalArgumentException("Autofill obsługiwany teraz tylko dla ELECTRICITY");
        }

        String text = pdfTextExtractor.extractText(pdf);
        var parsed = invoiceParser.parseForAutofillElectricity(text);

        String name = parsed.providerName();
        BigDecimal tax = parsed.taxPercent();

        var items = parsed.items() == null ? List.<InvoiceParser.InvoiceLine>of() : parsed.items();

        var fieldDtos = items.stream()
                .map(i -> new HousingProviderAutofillResponse.ProviderFieldDto(
                        i.name(), i.price(), i.billingMethod()
                ))
                .toList();

        return new HousingProviderAutofillResponse(
                name,
                ProviderType.ELECTRICITY,
                tax,
                null,
                fieldDtos
        );
    }
}
