package pl.rentowne.settlement;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO dla listy rozliczeń
 */
record SettlementListDto(
        Long id,
        String date, // data rozliczenia
        BigDecimal compensationAmount, // kwota odstępnego
        BigDecimal rentAmount, // kwota czynszu
        BigDecimal internetFee, // kwota za internet
        BigDecimal gasDeposit, // opłata za gaz
        BigDecimal electricityAmount, // kwota elektryczna z liczników
        BigDecimal distributedElectricityAmount, // kwota elektryczna z licznika mieszkania
        BigDecimal waterAmount, // kwota wody z liczników
        BigDecimal gasAmount, // kwota za gaz z liczników
        BigDecimal totalAmount, // kwota całkowita
        BigDecimal rentAndUtilitiesAmount, // Czynsz i media – kwota czynszu + internet + gaz + prąd + woda
        String prorationDescription, // opis proporcjonalności rozliczenia
        BigDecimal depositTopUp,
        BigDecimal additionalCharge,
        String additionalChargeDescription,
        List<SettlementChargeDto> additionalCharges
) {
}

