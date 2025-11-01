package pl.rentowne.meter.model.dto;

import pl.rentowne.meter.model.MeterType;

/**
 * DTO listy liczników
 */
public record MeterListDto(
        Long id,
        String apartmentName,
        String rentedObject,
        MeterType meterType,
        String meterNumber,
        boolean distributeReadings
) {
}
