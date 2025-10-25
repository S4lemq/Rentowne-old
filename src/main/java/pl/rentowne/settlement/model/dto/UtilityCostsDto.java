package pl.rentowne.settlement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO zawierające informacje o kosztach i zużyciu mediów
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilityCostsDto {

    private BigDecimal waterConsumption; //zużycie wody w m³
    private BigDecimal waterAmount; //kwota za wodę

    private BigDecimal electricityConsumption; //zużycie prądu w kWh
    private BigDecimal electricityAmount; //kwota za prąd
}

