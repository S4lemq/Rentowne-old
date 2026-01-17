package pl.rentowne.settlement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SettlementExportDto {

    private LocalDateTime date; // data rozliczenia
    private BigDecimal compensationAmount; // kwota odstępnego
    private BigDecimal rentAmount; // kwota czynszu
    private BigDecimal internetFee; // kwota za internet
    private BigDecimal gasDeposit; // opłata za gaz
    private BigDecimal electricityAmount; // kwota elektryczna z liczników
    private BigDecimal waterAmount; // kwota wody z liczników
    private BigDecimal totalAmount; // kwota całkowita
}
