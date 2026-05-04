package pl.rentowne.lease_agreement;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO zawierające informacje o kosztach i zużyciu przed zakończeniem umowy
 */
@Builder
record LeaseTerminationInfoDto(
        BigDecimal waterConsumption, //zużycie wody w m³
        BigDecimal waterAmount, //kwota za wodę
        BigDecimal electricityConsumption, //zużycie prądu w kWh
        BigDecimal electricityAmount, //kwota za prąd
        BigDecimal depositPaid, //wpłacona kaucja
        boolean hasPartialSettlement //czy było częściowe rozliczenie
) {}
