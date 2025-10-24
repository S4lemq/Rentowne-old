package pl.rentowne.settlement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyFinancialReportDto {

    private LocalDate month; // miesiąc
    private BigDecimal totalRevenue; // przychód
    private BigDecimal totalCosts; // koszty
    private BigDecimal netIncome; // dochód
    private BigDecimal netIncomeSum; // dochód suma
}

