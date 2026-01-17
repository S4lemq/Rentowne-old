package pl.rentowne.tenant.model.dto;

import lombok.Builder;
import lombok.Getter;
import pl.rentowne.address.AddressDto;
import pl.rentowne.address.AddressMapper;
import pl.rentowne.apartment.ApartmentDto;
import pl.rentowne.apartment.ApartmentMapper;
import pl.rentowne.lease_agreement.model.dto.LeaseAgreementDto;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.rented_object.RentedObjectDto;
import pl.rentowne.tenant.model.Tenant;

@Getter
@Builder
public class TenantDto {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String accountNumber;
    private String phoneNumber;
    private AddressDto addressDto;
    private LeaseAgreementDto leaseAgreementDto;
    private RentedObjectDto rentedObjectDto;
    private ApartmentDto apartment;


    public static Tenant asEntity(TenantDto dto) {
        return Tenant.builder()
                .id(dto.id)
                .firstname(dto.firstname)
                .lastname(dto.lastname)
                .email(dto.email)
                .accountNumber(dto.accountNumber)
                .phoneNumber(dto.phoneNumber)
                .address(dto.addressDto != null ? AddressMapper.toEntity(dto.addressDto) : null)
                .leaseAgreement(LeaseAgreementDto.asEntity(dto.getLeaseAgreementDto()))
                .rentedObject(new RentedObject(dto.getRentedObjectDto().getId()))
                .build();
    }

    public static TenantDto asDto(Tenant entity) {
        return TenantDto.builder()
                .id(entity.getId())
                .firstname(entity.getFirstname())
                .lastname(entity.getLastname())
                .email(entity.getEmail())
                .accountNumber(entity.getAccountNumber())
                .phoneNumber(entity.getPhoneNumber())
                .addressDto(entity.getAddress() != null ? AddressMapper.toDto(entity.getAddress()) : null)
                .leaseAgreementDto(LeaseAgreementDto.asDto(entity.getLeaseAgreement()))
                .rentedObjectDto(RentedObjectDto.asDtoWithoutMeters(entity.getRentedObject()))
                .apartment(ApartmentMapper.toBasicDto(entity.getRentedObject().getApartment()))
                .build();
    }

}
