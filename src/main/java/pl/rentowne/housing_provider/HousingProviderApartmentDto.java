package pl.rentowne.housing_provider;

import java.math.BigDecimal;

/**
 * DTO dla dostawców świadczeń przypisanych do konkretnego mieszkania
 */
record HousingProviderApartmentDto(
        Long id,
        String name,
        ProviderType type,
        BigDecimal tax
) {
}

