package pl.rentowne.tenant;

import java.time.LocalDateTime;

public record TenantRowDto(
        Long id,
        String firstname,
        String lastname,
        String apartmentName,
        String rentedObjectName,
        String phoneNumber,
        String email,
        LocalDateTime endContractDate
) {
}
