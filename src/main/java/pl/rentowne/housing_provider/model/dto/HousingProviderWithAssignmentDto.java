package pl.rentowne.housing_provider.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.rentowne.housing_provider.model.ProviderType;

import java.math.BigDecimal;

/**
 * DTO dla dostawców świadczeń z informacją o przypisaniu do mieszkania
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HousingProviderWithAssignmentDto {
    private Long id;
    private String name;
    private ProviderType type;
    private BigDecimal tax;
    private boolean assignedToApartment;
}

