package pl.rentowne.housing_provider;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import pl.rentowne.provider_field.model.ProviderField;
import pl.rentowne.provider_field.model.dto.ProviderFieldDto;
import pl.rentowne.user.model.User;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class HousingProviderMapper {

    static HousingProviderBasicDto toBasicDto(@NonNull final HousingProvider provider) {
        return new HousingProviderBasicDto(
                provider.getId(),
                provider.getName(),
                provider.getType(),
                provider.isRecurringPayment()
        );
    }

    static HousingProviderDto toDto(@NonNull final HousingProvider entity) {
        return new HousingProviderDto(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getTax(),
                entity.getConversionRate(),
                entity.getProviderFields().stream()
                        .map(HousingProviderMapper::toProviderFieldDto)
                        .toList(),
                entity.isRecurringPayment()
        );
    }

    static HousingProvider toEntity(@NonNull final HousingProviderDto dto, @NonNull final Long userId) {
        final HousingProvider housingProvider = HousingProvider.builder()
                .id(dto.id())
                .name(dto.name())
                .type(dto.type())
                .tax(dto.tax())
                .conversionRate(dto.conversionRate())
                .recurringPayment(dto.recurringPayment())
                .user(new User(userId))
                .build();

        final List<ProviderField> fields = dto.providerFieldDtos().stream()
                .map(field -> toProviderFieldEntity(field, housingProvider))
                .toList();

        housingProvider.setProviderFields(fields);
        return housingProvider;
    }

    private static ProviderFieldDto toProviderFieldDto(@NonNull final ProviderField field) {
        return new ProviderFieldDto(
                field.getId(),
                field.getName(),
                field.getPrice(),
                field.getBillingMethod()
        );
    }

    static ProviderField toProviderFieldEntity(@NonNull final ProviderFieldDto dto, @NonNull final HousingProvider housingProvider) {
        return ProviderField.builder()
                .id(dto.id())
                .name(dto.name())
                .price(dto.price())
                .billingMethod(dto.billingMethod())
                .housingProvider(housingProvider)
                .build();
    }

    static void updateEntity(@NonNull final HousingProvider entity, @NonNull final HousingProviderDto dto) {
        entity.setName(dto.name());
        entity.setType(dto.type());
        entity.setTax(dto.tax());
        entity.setConversionRate(dto.conversionRate());
        entity.setRecurringPayment(dto.recurringPayment());
    }

    static void updateProviderFieldEntity(@NonNull final ProviderField entity, @NonNull final ProviderFieldDto dto) {
        entity.setName(dto.name());
        entity.setPrice(dto.price());
        entity.setBillingMethod(dto.billingMethod());
    }
}
