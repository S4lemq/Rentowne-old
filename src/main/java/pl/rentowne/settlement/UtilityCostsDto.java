package pl.rentowne.settlement;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO zawierające informacje o kosztach i zużyciu mediów
 */
@Builder
public record UtilityCostsDto(
        BigDecimal waterConsumption, //zużycie wody w m³
        BigDecimal waterAmount, //kwota za wodę
        BigDecimal electricityConsumption, //zużycie prądu w kWh
        BigDecimal electricityAmount //kwota za prąd
) {
}
