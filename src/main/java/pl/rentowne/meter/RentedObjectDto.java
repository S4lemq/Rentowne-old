package pl.rentowne.meter;

import lombok.Builder;

@Builder
public record RentedObjectDto(
        Long id,
        String rentedObjectName
) {
}

