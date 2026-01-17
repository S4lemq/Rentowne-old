package pl.rentowne.apartment;

import lombok.Builder;

@Builder
record RentedObjectDto(
        Long id,
        String rentedObjectName
) {
}
