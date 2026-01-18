package pl.rentowne.meter;

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
