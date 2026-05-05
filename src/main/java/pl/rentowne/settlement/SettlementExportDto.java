package pl.rentowne.settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

record SettlementExportDto(
        LocalDateTime date,
        BigDecimal compensationAmount,
        BigDecimal rentAmount,
        BigDecimal internetFee,
        BigDecimal gasDeposit,
        BigDecimal electricityAmount,
        BigDecimal waterAmount,
        BigDecimal totalAmount
) {
}
