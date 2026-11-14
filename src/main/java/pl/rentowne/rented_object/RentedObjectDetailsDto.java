package pl.rentowne.rented_object;

import lombok.Builder;
import pl.rentowne.rental_payment.RentalBalanceDto;

import java.math.BigDecimal;

@Builder(toBuilder = true)
record RentedObjectDetailsDto(
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
        boolean hasCurrentMonthSettlement, // czy ma rozliczenie z bieżącego miesiąca
        RentalBalanceDto paymentSummary
) {
}
