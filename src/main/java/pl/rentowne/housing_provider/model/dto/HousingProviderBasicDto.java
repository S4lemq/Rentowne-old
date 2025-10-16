package pl.rentowne.housing_provider.model.dto;

import lombok.Builder;
import lombok.Getter;
import pl.rentowne.housing_provider.model.ProviderType;

@Getter
@Builder
public class HousingProviderBasicDto {
    private Long id;
    private String name;
    private ProviderType type;
}

