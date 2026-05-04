package pl.rentowne.meter_reading;

import pl.rentowne.meter.MeterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record MeterReadingRowDto(
        Long id,
        BigDecimal currentReading,
        LocalDateTime readingDate,
        BigDecimal consumption,
        boolean settled,
        MeterType meterType
) {}
