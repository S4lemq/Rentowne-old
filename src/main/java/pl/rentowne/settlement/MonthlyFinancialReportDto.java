package pl.rentowne.settlement;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
record MonthlyFinancialReportDto(
        LocalDate month,
        BigDecimal totalRevenue,
        BigDecimal totalCosts,
        BigDecimal netIncome,
        BigDecimal netIncomeSum
) {
}
