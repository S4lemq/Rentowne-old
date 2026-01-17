package pl.rentowne.address;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressMapper {

    public static AddressDto toDto(final @NonNull Address entity) {
        return new AddressDto(
                entity.getId(),
                entity.getStreetName(),
                entity.getBuildingNumber(),
                entity.getApartmentNumber(),
                entity.getZipCode(),
                entity.getCityName(),
                entity.getVoivodeship()
        );
    }

    public static Address toEntity(final @NonNull AddressDto dto) {
        return Address.builder()
                .id(dto.id())
                .streetName(dto.streetName())
                .buildingNumber(dto.buildingNumber())
                .apartmentNumber(dto.apartmentNumber())
                .zipCode(dto.zipCode())
                .cityName(dto.cityName())
                .voivodeship(dto.voivodeship())
                .build();
    }
}
