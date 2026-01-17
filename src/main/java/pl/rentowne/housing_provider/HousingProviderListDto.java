package pl.rentowne.housing_provider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

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
