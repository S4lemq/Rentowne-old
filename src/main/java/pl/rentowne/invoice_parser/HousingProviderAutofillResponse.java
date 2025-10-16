package pl.rentowne.invoice_parser;

import pl.rentowne.housing_provider.model.ProviderType;
import pl.rentowne.provider_field.model.BillingMethod;

import java.math.BigDecimal;
import java.util.List;

public record HousingProviderAutofillResponse(
        String name,
        ProviderType type,
        BigDecimal tax,
        BigDecimal conversionRate,
        List<ProviderFieldDto> providerFieldDtos
) {
    public record ProviderFieldDto(
            String name,
            BigDecimal price,
            BillingMethod billingMethod
    ) {}
}