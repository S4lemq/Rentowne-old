package pl.rentowne.meter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApartmentMeterDto(
        Long id,
        String name,
        MeterType meterType,
        ApartmentDto apartment,
        String meterNumber,
        BigDecimal initialMeterReading,
        LocalDateTime installationDate,
        boolean distributeReadings
) {
}
