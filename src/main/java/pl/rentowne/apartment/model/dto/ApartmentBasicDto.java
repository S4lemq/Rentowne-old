package pl.rentowne.apartment.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApartmentBasicDto {
    private Long id;
    private String apartmentName;
}
