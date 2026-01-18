package pl.rentowne.invoice_parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneErrorCode;
import pl.rentowne.housing_provider.HousingProvider;
import pl.rentowne.housing_provider.HousingProviderRepository;
import pl.rentowne.housing_provider.ProviderType;
import pl.rentowne.provider_field.model.ProviderField;
import pl.rentowne.user.service.UserService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceComparisonService {

    private final HousingProviderRepository housingProviderRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final InvoiceParser invoiceParser;
    private final UserService userService;

    @Transactional(readOnly = true)
    public InvoiceComparisonResponse compareInvoiceWithSaved(Long housingProviderId, MultipartFile pdf, ProviderType type) {
        // Walidacja - czy użytkownik jest właścicielem tego HousingProvider
        HousingProvider housingProvider = housingProviderRepository.findWithProviderFieldsById(housingProviderId)
                .orElseThrow(() -> new RentowneBusinessException(RentowneErrorCode.HOUSING_PROVIDER_NOT_FOUND));

        Long loggedUserId = userService.getLoggedUser().getId();
        if (!housingProvider.getUser().getId().equals(loggedUserId)) {
            throw new RentowneBusinessException(RentowneErrorCode.NO_PERMISSION_TO_HOUSING_PROVIDER);
        }

        if (type != ProviderType.ELECTRICITY) {
            throw new IllegalArgumentException("Porównywanie obsługiwane teraz tylko dla ELECTRICITY");
        }

        // Parsowanie nowej faktury
        String text = pdfTextExtractor.extractText(pdf);
        var parsedInvoice = invoiceParser.parseForAutofillElectricity(text);

        // Porównanie danych podstawowych
        InvoiceComparisonResponse.BasicInfoChanges basicInfoChanges = compareBasicInfo(
                housingProvider,
                parsedInvoice.taxPercent()
        );

        // Porównanie pól
        List<ProviderField> existingFields = housingProvider.getProviderFields();
        List<InvoiceParser.InvoiceLine> newFields = parsedInvoice.items() == null ?
                List.of() : parsedInvoice.items();

        var fieldComparison = compareFields(existingFields, newFields);

        boolean hasChanges = basicInfoChanges.hasAnyChange() ||
                !fieldComparison.modified.isEmpty() ||
                !fieldComparison.added.isEmpty() ||
                !fieldComparison.removed.isEmpty();

        return new InvoiceComparisonResponse(
                basicInfoChanges,
                fieldComparison.modified,
                fieldComparison.added,
                fieldComparison.removed,
                hasChanges
        );
    }

    private InvoiceComparisonResponse.BasicInfoChanges compareBasicInfo(
            HousingProvider existing,
            BigDecimal newTax) {

        InvoiceComparisonResponse.ValueChange<BigDecimal> taxChange = null;
        if (newTax != null && (existing.getTax() == null || existing.getTax().compareTo(newTax) != 0)) {
            taxChange = new InvoiceComparisonResponse.ValueChange<>(existing.getTax(), newTax);
        }

        return new InvoiceComparisonResponse.BasicInfoChanges(taxChange, null);
    }

    private record FieldComparisonResult(
            List<InvoiceComparisonResponse.FieldChange> modified,
            List<InvoiceComparisonResponse.AddedField> added,
            List<InvoiceComparisonResponse.RemovedField> removed
    ) {}

    private FieldComparisonResult compareFields(
            List<ProviderField> existingFields,
            List<InvoiceParser.InvoiceLine> newFields) {

        Map<String, ProviderField> existingByName = existingFields.stream()
                .collect(Collectors.toMap(
                        field -> normalizeFieldName(field.getName()),
                        field -> field,
                        (a, b) -> a
                ));

        Map<String, InvoiceParser.InvoiceLine> newByName = newFields.stream()
                .collect(Collectors.toMap(
                        field -> normalizeFieldName(field.name()),
                        field -> field,
                        (a, b) -> a
                ));

        List<InvoiceComparisonResponse.FieldChange> modified = new ArrayList<>();
        List<InvoiceComparisonResponse.AddedField> added = new ArrayList<>();
        List<InvoiceComparisonResponse.RemovedField> removed = new ArrayList<>();

        // Sprawdź zmiany i usunięcia
        for (Map.Entry<String, ProviderField> entry : existingByName.entrySet()) {
            String normalizedName = entry.getKey();
            ProviderField existingField = entry.getValue();

            if (newByName.containsKey(normalizedName)) {
                InvoiceParser.InvoiceLine newField = newByName.get(normalizedName);

                boolean priceChanged = existingField.getPrice().compareTo(newField.price()) != 0;
                boolean billingMethodChanged = !existingField.getBillingMethod().equals(newField.billingMethod());

                if (priceChanged || billingMethodChanged) {
                    modified.add(new InvoiceComparisonResponse.FieldChange(
                            existingField.getName(),
                            existingField.getPrice(),
                            newField.price(),
                            existingField.getBillingMethod(),
                            newField.billingMethod()
                    ));
                }
            } else {
                removed.add(new InvoiceComparisonResponse.RemovedField(
                        existingField.getName(),
                        existingField.getPrice(),
                        existingField.getBillingMethod()
                ));
            }
        }

        // Sprawdź dodane pola
        for (Map.Entry<String, InvoiceParser.InvoiceLine> entry : newByName.entrySet()) {
            String normalizedName = entry.getKey();
            InvoiceParser.InvoiceLine newField = entry.getValue();

            if (!existingByName.containsKey(normalizedName)) {
                added.add(new InvoiceComparisonResponse.AddedField(
                        newField.name(),
                        newField.price(),
                        newField.billingMethod()
                ));
            }
        }

        return new FieldComparisonResult(modified, added, removed);
    }

    private String normalizeFieldName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase();
    }
}

