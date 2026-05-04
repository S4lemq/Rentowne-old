package pl.rentowne.housing_provider;

import pl.rentowne.provider_field.ProviderFieldDto;

import java.math.BigDecimal;
import java.util.List;

record HousingProviderDto(
        Long id,
        String name,
        ProviderType type,
        BigDecimal tax,
        BigDecimal conversionRate,
        List<ProviderFieldDto> providerFieldDtos,
        boolean recurringPayment
) {
}
