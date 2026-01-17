package pl.rentowne.apartment;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.lang.Nullable;
import pl.rentowne.address.AddressMapper;
import pl.rentowne.rented_object.RentedObject;
import pl.rentowne.user.model.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ApartmentMapper {

    public static ApartmentDto toDto(@NonNull final Apartment apartment) {
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
                .map(ApartmentMapper::toDto)
                .toList())
            .build();
    }

    public static ApartmentDto toBasicDto(@NonNull final Apartment apartment) {
        return ApartmentDto.builder()
            .id(apartment.getId())
            .apartmentName(apartment.getApartmentName())
            .build();
    }

    public static Apartment toEntity(@NonNull final ApartmentDto dto, @Nullable final Long apartmentId, @NonNull final Long userId) {
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

    public static RentedObjectDto toDto(@NonNull final RentedObject rentedObject) {
        return new RentedObjectDto(
                rentedObject.getId(),
                rentedObject.getRentedObjectName()
        );
    }

    public static Set<RentedObject> toRentedObjectEntities(@NonNull final ApartmentDto apartmentDto, @NonNull final Long apartmentId) {
        final List<RentedObjectDto> rentedObjectDtos = apartmentDto.rentedObjectDtos();
        if (rentedObjectDtos == null || rentedObjectDtos.isEmpty()) {
            return new HashSet<>();
        }

        return rentedObjectDtos.stream()
            .map(dto -> RentedObject.builder()
                .rentedObjectName(dto.rentedObjectName())
                .isRented(false)
                .apartment(new Apartment(apartmentId))
                .build())
            .collect(Collectors.toSet());
    }
}
