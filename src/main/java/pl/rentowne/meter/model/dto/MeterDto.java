package pl.rentowne.meter.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.rentowne.apartment.ApartmentDto;
import pl.rentowne.meter.model.MeterType;
import pl.rentowne.rented_object.model.dto.RentedObjectDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterDto {
    private Long id;
    private String name;
    private MeterType meterType;
    private RentedObjectDto rentedObject;
    private ApartmentDto apartment;
    private String meterNumber;
    private BigDecimal initialMeterReading;
    private LocalDateTime installationDate;
    private boolean distributeReadings;

    public MeterDto(Long id) {
        this.id = id;
    }
}
