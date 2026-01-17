package pl.rentowne.meter;

import lombok.Builder;

@Builder
public record ApartmentDto(
        Long id,
        String apartmentName
) {
}
