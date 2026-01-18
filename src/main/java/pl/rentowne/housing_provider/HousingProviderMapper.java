package pl.rentowne.housing_provider;

import org.springframework.stereotype.Component;
import pl.rentowne.provider_field.model.ProviderField;
import pl.rentowne.provider_field.model.dto.ProviderFieldDto;
import pl.rentowne.user.model.User;

import java.util.List;

@Component
class HousingProviderMapper {

    HousingProviderBasicDto toBasicDto(HousingProvider provider) {
        return new HousingProviderBasicDto(
                provider.getId(),
                provider.getName(),
                provider.getType(),
                provider.isRecurringPayment()
        );
    }

    HousingProviderDto toDto(HousingProvider entity) {
        return new HousingProviderDto(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getTax(),
                entity.getConversionRate(),
                entity.getProviderFields().stream()
                        .map(this::toProviderFieldDto)
                        .toList(),
                entity.isRecurringPayment()
        );
    }

    HousingProvider toEntity(HousingProviderDto dto, Long userId) {
        HousingProvider housingProvider = HousingProvider.builder()
                .id(dto.id())
                .name(dto.name())
                .type(dto.type())
                .tax(dto.tax())
                .conversionRate(dto.conversionRate())
                .recurringPayment(dto.recurringPayment())
                .user(new User(userId))
                .build();

        List<ProviderField> fields = dto.providerFieldDtos().stream()
                .map(field -> toProviderField(field, housingProvider))
                .toList();

        housingProvider.setProviderFields(fields);
        return housingProvider;
    }

    private ProviderFieldDto toProviderFieldDto(ProviderField field) {
        return new ProviderFieldDto(
                field.getId(),
                field.getName(),
                field.getPrice(),
                field.getBillingMethod()
        );
    }

    private ProviderField toProviderField(ProviderFieldDto dto, HousingProvider housingProvider) {
        return ProviderField.builder()
                .id(dto.id())
                .name(dto.name())
                .price(dto.price())
                .billingMethod(dto.billingMethod())
                .housingProvider(housingProvider)
                .build();
    }
}
