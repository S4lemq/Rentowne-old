package pl.rentowne.settlement;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
record YearlyFinancialReportDto(
        List<MonthlyFinancialReportDto> months,  // lista miesięcznych raportów
        BigDecimal yearlyTotalRevenue, // suma przychodów za rok/okres
        BigDecimal yearlyTotalCosts // suma kosztów za rok/okres
) {
}
