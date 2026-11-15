package pl.rentowne.settlement;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
record YearlyFinancialReportDto(
        List<MonthlyFinancialReportDto> months,  // lista miesięcznych raportów
        BigDecimal yearlyTotalRevenue, // suma przychodów za rok/okres
        BigDecimal yearlyTotalCosts, // legacy: includes estimated tax
        pl.rentowne.analytics.AnalyticsDto.Totals metrics,
        pl.rentowne.analytics.AnalyticsDto.Basis basis,
        java.time.LocalDate asOf,
        java.time.LocalDate firstMonth,
        List<pl.rentowne.analytics.AnalyticsDto.Apartment> apartments,
        List<pl.rentowne.analytics.AnalyticsDto.Month> timeline
) {
}
