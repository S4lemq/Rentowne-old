package pl.rentowne.provider_field.model.dto;

import pl.rentowne.provider_field.model.BillingMethod;

import java.math.BigDecimal;

public record ProviderFieldDto(
        Long id,
        String name,
        BigDecimal price,
        BillingMethod billingMethod
) {
}
