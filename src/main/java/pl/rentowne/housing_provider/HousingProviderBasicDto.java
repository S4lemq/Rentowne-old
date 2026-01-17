package pl.rentowne.housing_provider;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HousingProviderBasicDto {
    private Long id;
    private String name;
    private ProviderType type;
    private boolean recurringPayment;
}
