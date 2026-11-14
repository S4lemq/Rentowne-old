package pl.rentowne.meter_reading;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record AddMeterReadingRequest(
        @NotNull @Positive Long meterId,
        @NotNull @DecimalMin("0") @DecimalMax("99999") @Digits(integer = 5, fraction = 5) BigDecimal currentReading,
        @NotNull LocalDateTime readingDate
) {}
