package pl.rentowne.lease_agreement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO zawierające informacje o kosztach i zużyciu przed zakończeniem umowy
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaseTerminationInfoDto {

    private BigDecimal waterConsumption; //zużycie wody w m³
    private BigDecimal waterAmount; //kwota za wodę

    private BigDecimal electricityConsumption; //zużycie prądu w kWh
    private BigDecimal electricityAmount; //kwota za prąd

    private BigDecimal depositPaid; //wpłacona kaucja

    private boolean hasPartialSettlement; //czy było częściowe rozliczenie
}
