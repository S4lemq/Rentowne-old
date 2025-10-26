package pl.rentowne.rented_object.model.dto;

import java.math.BigDecimal;

/**
 * DTO zawierające szczegółowe informacje o obiekcie do wynajęcia wraz z danymi rozliczenia
 */
public record RentedObjectDetailsDto(
        Long id,
        String objectName, // obiekt
        String tenantFullName, // imię i nazwisko
        Long tenantId, // id najemcy
        String lastSettlementDate, // data ostatniego rozliczenia
        BigDecimal totalAmount, // kwota całkowita
        BigDecimal compensationAmount, // kwota odstępnego
        BigDecimal sum, // suma
        BigDecimal rentAmount, // kwota czynszu
        BigDecimal internetFee, // kwota za internet
        BigDecimal electricityAmount, // kwota za prąd
        BigDecimal gasDeposit, // kwota za gaz
        BigDecimal waterAmount, // kwota za wodę
        BigDecimal profit, // zysk
        boolean hasCurrentMonthSettlement // czy ma rozliczenie z bieżącego miesiąca
) {
}
