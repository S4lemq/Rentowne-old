package pl.rentowne.address;

import lombok.NonNull;
import org.springframework.lang.Nullable;

public record AddressDto(
        @Nullable Long id,
        @Nullable String streetName,
        @NonNull String buildingNumber,
        @Nullable String apartmentNumber,
        @NonNull String zipCode,
        @NonNull String cityName,
        @NonNull String voivodeship
) {
}
