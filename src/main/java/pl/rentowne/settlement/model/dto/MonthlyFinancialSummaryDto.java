package pl.rentowne.settlement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyFinancialSummaryDto {

    private BigDecimal totalAdministrativeFee;
    private BigDecimal totalInternet;
    private BigDecimal totalElectricity;
    private BigDecimal totalGas;
    private BigDecimal totalTax;
    private BigDecimal totalRevenue;
    private BigDecimal totalCosts;
    private BigDecimal netIncome;
    private BigDecimal cumulativeNetIncome;
}

