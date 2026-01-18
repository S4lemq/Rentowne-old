package pl.rentowne.meter;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
