package pl.rentowne.tenant;

import lombok.Builder;
import pl.rentowne.address.AddressDto;
import pl.rentowne.address.AddressMapper;
import pl.rentowne.lease_agreement.LeaseAgreementDto;
import pl.rentowne.rented_object.RentedObject;

@Builder
public record TenantDto(
        Long id,
        String firstname,
        String lastname,
        String email,
        String accountNumber,
        String phoneNumber,
        AddressDto addressDto,
        LeaseAgreementDto leaseAgreementDto,
        RentedObjectDto rentedObjectDto,
        ApartmentDto apartment
) {

    public static Tenant asEntity(TenantDto dto) {
        return Tenant.builder()
                .id(dto.id())
                .firstname(dto.firstname())
                .lastname(dto.lastname())
                .email(dto.email())
                .accountNumber(dto.accountNumber())
                .phoneNumber(dto.phoneNumber())
                .address(dto.addressDto() != null ? AddressMapper.toEntity(dto.addressDto()) : null)
                .leaseAgreement(LeaseAgreementDto.asEntity(dto.leaseAgreementDto()))
                .rentedObject(new RentedObject(dto.rentedObjectDto().id()))
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
                .rentedObjectDto(RentedObjectDto.builder()
                        .id(entity.getRentedObject().getId())
                        .rentedObjectName(entity.getRentedObject().getRentedObjectName())
                        .build())
                .apartment(ApartmentDto.builder()
                        .id(entity.getRentedObject().getApartment().getId())
                        .apartmentName(entity.getRentedObject().getApartment().getApartmentName())
                        .build())
                .build();
    }

}
