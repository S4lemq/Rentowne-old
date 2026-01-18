package pl.rentowne.meter;

import java.math.BigDecimal;

public record MeterReadingData(Long id, BigDecimal consumption) {
}
