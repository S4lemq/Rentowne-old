package pl.rentowne.invoice_parser;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pl.rentowne.housing_provider.model.ProviderType;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/housing-service-provider")
public class HousingProviderAutofillController {

    private final InvoiceAutofillService autofillService;

    @PostMapping(path = "/invoice/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public HousingProviderAutofillResponse parseInvoiceForAutofill(@RequestPart("file") MultipartFile file,
                                                                   @RequestParam(name = "type", defaultValue = "ELECTRICITY") ProviderType type) {
        return autofillService.autofillFromPdf(file, type);
    }
}