package pl.rentowne.apartment.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigInteger;

@Getter
@AllArgsConstructor
public class ApartmentRowDto {
    private Long id;
    private String apartmentName;
    private BigInteger leasesNumber;
    private boolean isRented;
    private String cityName;
    private String streetName;
    private String buildingNumber;
    private String apartmentNumber;
    private String image;
    private boolean pinned;
}
