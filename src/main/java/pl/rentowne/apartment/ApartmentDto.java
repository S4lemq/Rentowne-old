package pl.rentowne.apartment;

import lombok.Builder;
import jakarta.validation.Valid;
import org.springframework.lang.Nullable;
import pl.rentowne.address.AddressDto;
import pl.rentowne.owner.OwnerDto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Builder
record ApartmentDto(
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
    @Nullable List<RentedObjectDto> rentedObjectDtos,
    @Nullable @Valid OwnerDto ownerDto
) {}
