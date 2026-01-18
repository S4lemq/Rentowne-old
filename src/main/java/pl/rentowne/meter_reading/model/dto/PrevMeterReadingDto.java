package pl.rentowne.meter_reading.model.dto;

import pl.rentowne.meter.MeterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PrevMeterReadingDto(
        Long id,
        BigDecimal currentReading,
        LocalDateTime readingDate,
        String tenantFullName,
        String rentedObjectName,
        MeterType meterType
) {
}
