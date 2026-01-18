package pl.rentowne.housing_provider;

import java.math.BigDecimal;

record HousingProviderListDto(
        Long id,
        String name,
        ProviderType type,
        BigDecimal tax,
        String apartmentName
) {
}
