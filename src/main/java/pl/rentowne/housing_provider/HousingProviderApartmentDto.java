package pl.rentowne.housing_provider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO dla dostawców świadczeń przypisanych do konkretnego mieszkania
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HousingProviderApartmentDto {
    private Long id;
    private String name;
    private ProviderType type;
    private BigDecimal tax;
}

