package pl.rentowne.owner;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OwnerMapper {
    public static OwnerDto toDto(Owner owner) {
        if (owner == null) return null;
        return new OwnerDto(owner.getId(), owner.getVersion(), owner.getFirstName(), owner.getLastName(),
                owner.getEmail(), owner.getStreetName(), owner.getBuildingNumber(), owner.getApartmentNumber(),
                owner.getZipCode(), owner.getCityName(), owner.getRepresentativeName(), owner.getRepresentativeEmail(),
                owner.getAccountNumber(), owner.getPaymentAccountHolder());
    }

    static void update(Owner owner, OwnerDto dto) {
        owner.setFirstName(dto.firstName());
        owner.setLastName(dto.lastName());
        owner.setEmail(dto.email());
        owner.setStreetName(dto.streetName());
        owner.setBuildingNumber(dto.buildingNumber());
        owner.setApartmentNumber(dto.apartmentNumber());
        owner.setZipCode(dto.zipCode());
        owner.setCityName(dto.cityName());
        owner.setRepresentativeName(dto.representativeName());
        owner.setRepresentativeEmail(dto.representativeEmail());
        owner.setAccountNumber(dto.accountNumber());
        owner.setPaymentAccountHolder(dto.paymentAccountHolder());
    }
}
