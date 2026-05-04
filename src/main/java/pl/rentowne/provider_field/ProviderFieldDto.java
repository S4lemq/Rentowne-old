package pl.rentowne.provider_field;

import java.math.BigDecimal;

public record ProviderFieldDto(
        Long id,
        String name,
        BigDecimal price,
        BillingMethod billingMethod
) {
}
