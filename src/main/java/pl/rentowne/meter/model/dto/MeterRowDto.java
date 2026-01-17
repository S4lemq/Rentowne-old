package pl.rentowne.meter.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.rentowne.meter.model.MeterType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MeterRowDto {
    private Long id;
    private String apartment;
    private String rentedObject;
    private String meterName;
    private MeterType meterType;
    private String meterNumber;
    private BigDecimal initialMeterReading;
    private LocalDateTime installationDate;
}
