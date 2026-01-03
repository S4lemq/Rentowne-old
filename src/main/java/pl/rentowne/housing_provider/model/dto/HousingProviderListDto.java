package pl.rentowne.housing_provider.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import pl.rentowne.housing_provider.model.ProviderType;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class HousingProviderListDto {
    private Long id;
    private String name;
    private ProviderType type;
    private BigDecimal tax;
    private String apartmentName;
}
