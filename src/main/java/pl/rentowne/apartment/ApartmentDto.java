package pl.rentowne.apartment;

import lombok.Builder;
import org.springframework.lang.Nullable;
import pl.rentowne.address.AddressDto;
import pl.rentowne.rented_object.RentedObjectDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Builder
public record ApartmentDto(
    @Nullable Long id,
    @Nullable String apartmentName,
    @Nullable BigInteger leasesNumber,
    boolean isRented,
    @Nullable BigDecimal area,
    @Nullable String image,
    boolean pinned,
    @Nullable String cityName,
    @Nullable String streetName,
    @Nullable String buildingNumber,
    @Nullable String apartmentNumber,
    @Nullable AddressDto addressDto,
    @Nullable List<RentedObjectDto> rentedObjectDtos
) {}
