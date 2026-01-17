package pl.rentowne.rented_object;

import lombok.Builder;
import pl.rentowne.meter.model.MeterType;

import java.util.List;

@Builder
public record RentedObjectDto(
    Long id,
    String rentedObjectName,
    List<MeterDto> meters
) {
    record MeterDto(
            Long id,
            String name,
            String meterNumber,
            MeterType meterType
    ) {
    }
}
