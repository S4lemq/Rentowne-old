package pl.rentowne.tenant.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TenantRowDto {

    private Long id;
    private String firstname;
    private String lastname;
    private String apartmentName;
    private String rentedObjectName;
    private String phoneNumber;
    private String email;
    private LocalDateTime endContractDate;
}
