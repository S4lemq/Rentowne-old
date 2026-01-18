package pl.rentowne.meter_reading.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.rentowne.meter.MeterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MeterReadingRowDto {
    private Long id;
    private BigDecimal currentReading;
    private LocalDateTime readingDate;
    private BigDecimal consumption;
    private boolean settled;
    private MeterType meterType;
}
