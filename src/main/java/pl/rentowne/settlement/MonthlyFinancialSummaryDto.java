package pl.rentowne.settlement;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
record MonthlyFinancialSummaryDto(
        BigDecimal totalAdministrativeFee,
        BigDecimal totalInternet,
        BigDecimal totalElectricity,
        BigDecimal totalGas,
        BigDecimal totalTax,
        BigDecimal totalRevenue,
        BigDecimal totalCosts,
        BigDecimal netIncome,
        BigDecimal cumulativeNetIncome
) {
}
