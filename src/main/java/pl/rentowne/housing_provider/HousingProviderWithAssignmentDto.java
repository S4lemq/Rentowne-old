package pl.rentowne.housing_provider;

import java.math.BigDecimal;

/**
 * DTO dla dostawców świadczeń z informacją o przypisaniu do mieszkania
 */
record HousingProviderWithAssignmentDto(
        Long id,
        String name,
        ProviderType type,
        BigDecimal tax,
        boolean assignedToApartment
) {
}

