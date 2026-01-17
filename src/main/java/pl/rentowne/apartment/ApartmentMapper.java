package pl.rentowne.apartment;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.lang.Nullable;
import pl.rentowne.address.AddressMapper;
import pl.rentowne.rented_object.RentedObjectDto;
import pl.rentowne.user.model.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApartmentMapper {

    public static ApartmentDto toDto(@NonNull Apartment apartment) {
        return ApartmentDto.builder()
            .id(apartment.getId())
            .apartmentName(apartment.getApartmentName())
            .leasesNumber(apartment.getLeasesNumber())
            .isRented(apartment.isRented())
            .area(apartment.getArea())
            .image(apartment.getImage())
            .pinned(apartment.isPinned())
            .addressDto(AddressMapper.toDto(apartment.getAddress()))
            .rentedObjectDtos(apartment.getRentedObjects().stream()
                .map(RentedObjectDto::asDtoWithoutMeters)
                .toList())
            .build();
    }

    public static ApartmentDto toBasicDto(@NonNull Apartment apartment) {
        return ApartmentDto.builder()
            .id(apartment.getId())
            .apartmentName(apartment.getApartmentName())
            .build();
    }

    public static Apartment toEntity(@NonNull ApartmentDto dto, @Nullable Long apartmentId, @NonNull Long userId) {
        return Apartment.builder()
            .id(apartmentId)
            .apartmentName(dto.apartmentName())
            .leasesNumber(dto.leasesNumber())
            .isRented(dto.isRented())
            .area(dto.area())
            .image(dto.image())
            .user(new User(userId))
            .address(AddressMapper.toEntity(dto.addressDto()))
            .build();
    }
}
