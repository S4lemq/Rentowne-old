package pl.rentowne.rented_object.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.rentowne.meter.model.dto.MeterDto;
import pl.rentowne.rented_object.model.RentedObject;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentedObjectDto {
    private Long id;
    private String rentedObjectName;
    private boolean isRented;
    private List<MeterDto> meters;

    public RentedObjectDto(Long id, String rentedObjectName) {
        this.id = id;
        this.rentedObjectName = rentedObjectName;
    }

    public RentedObjectDto(Long id, String rentedObjectName, List<MeterDto> meters) {
        this.id = id;
        this.rentedObjectName = rentedObjectName;
        this.meters = meters;
    }


    public static RentedObject asEntityWithoutMeters(RentedObjectDto dto) {
        return RentedObject.builder()
                .id(dto.getId())
                .rentedObjectName(dto.getRentedObjectName())
                .isRented(dto.isRented)
                .build();
    }

    public static RentedObjectDto asDtoWithoutMeters(RentedObject rentedObject) {
        return RentedObjectDto.builder()
                .id(rentedObject.getId())
                .rentedObjectName(rentedObject.getRentedObjectName())
                .build();
    }
}

