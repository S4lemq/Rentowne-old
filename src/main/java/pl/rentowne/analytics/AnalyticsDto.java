package pl.rentowne.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDto {
    private AnalyticsDto() {}
    public enum Basis { ACCRUAL, CASH }
    public record Apartment(long id, String name) {}
    public record Totals(BigDecimal revenue, BigDecimal costs, BigDecimal result, BigDecimal margin,
                         BigDecimal estimatedTax, BigDecimal afterTax) {}
    public record Month(LocalDate month, Totals totals, BigDecimal cumulativeResult,
                        boolean hasRecords, boolean inProgress, boolean future) {}
    public record Category(String key, BigDecimal amount, BigDecimal share) {}
    public record Property(long id, String name, Totals totals, List<Month> months, List<Category> costs) {}
    public record ComparisonMonth(LocalDate month, BigDecimal current, BigDecimal previous,
                                  boolean currentHasRecords, boolean previousHasRecords) {}
    public record Comparison(List<ComparisonMonth> months, BigDecimal currentResult, BigDecimal previousResult,
                             BigDecimal changePercent, boolean available) {}
    public record Overview(LocalDate asOf, LocalDate from, LocalDate to, LocalDate firstMonth,
                           Basis basis, List<Apartment> apartments, Totals summary, List<Month> months,
                           List<Category> costs, List<Property> properties, Comparison comparison) {}
    public record ForecastMonth(LocalDate month, BigDecimal revenue, BigDecimal costs, BigDecimal result,
                                BigDecimal estimatedTax, BigDecimal afterTax, boolean complete,
                                List<String> missingData) {}
    public record EndingLease(String tenantName, String apartmentName, String objectName, LocalDate endDate,
                              BigDecimal monthlyCharges) {}
    public record Forecast(LocalDate asOf, int horizon, boolean continueTenancy, BigDecimal costChangePercent,
                           List<Month> history, List<ForecastMonth> months, List<EndingLease> endingLeases,
                           List<String> assumptions, boolean complete) {}
}
