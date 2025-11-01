package pl.rentowne.settlement.model.dto;

import java.math.BigDecimal;

/**
 * DTO dla listy rozliczeń
 */
public record SettlementListDto(
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
        BigDecimal rentAndUtilitiesAmount // Czynsz i media – kwota czynszu + internet + gaz + prąd + woda
) {
}
