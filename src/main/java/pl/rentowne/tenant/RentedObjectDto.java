package pl.rentowne.tenant;

import lombok.Builder;

@Builder
public record RentedObjectDto(
        Long id,
        String rentedObjectName
) {
}

