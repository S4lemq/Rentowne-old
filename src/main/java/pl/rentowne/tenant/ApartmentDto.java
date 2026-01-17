package pl.rentowne.tenant;

import lombok.Builder;

@Builder
public record ApartmentDto(
        Long id,
        String apartmentName
) {
}
