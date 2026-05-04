package pl.rentowne.meter_reading;

import pl.rentowne.meter.MeterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record PrevMeterReadingDto(
        Long id,
        BigDecimal currentReading,
        LocalDateTime readingDate,
        String tenantFullName,
        String rentedObjectName,
        MeterType meterType
) {
}
