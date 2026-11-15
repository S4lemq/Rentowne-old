package pl.rentowne.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.rentowne.analytics.AnalyticsDto.*;
import pl.rentowne.analytics.FinancialAnalyticsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static java.math.BigDecimal.ZERO;

/** Compatibility adapter: old after-tax fields remain, new fields explicitly expose the operating result. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class FinancialReportService {
    private final FinancialAnalyticsService analytics;

    public YearlyFinancialReportDto getMonthlyFinancialReport() { return getMonthlyFinancialReport(null); }
    public YearlyFinancialReportDto getMonthlyFinancialReport(Integer year) { return getMonthlyFinancialReport(year,null,Basis.ACCRUAL); }

    public YearlyFinancialReportDto getMonthlyFinancialReport(Integer year,Long apartmentId,Basis basis) {
        if (year != null && (year < 1900 || year > 2200)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Nieprawidłowy rok.");
        LocalDate from=year==null?null:LocalDate.of(year,1,1);
        LocalDate to=year==null?null:LocalDate.of(year,12,1);
        var data=analytics.report(from,to,apartmentId,basis,true);
        var rows=new ArrayList<MonthlyFinancialReportDto>();
        BigDecimal cumulative=ZERO;
        for(var month:data.months()) {
            if(!month.hasRecords()) continue;
            var t=month.totals();
            cumulative=cumulative.add(afterTax(t));
            rows.add(MonthlyFinancialReportDto.builder().month(month.month()).totalRevenue(t.revenue())
                    .totalCosts(legacyCosts(t)).netIncome(afterTax(t)).netIncomeSum(cumulative)
                    .operatingCosts(t.costs()).operatingResult(t.result()).cumulativeOperatingResult(month.cumulativeResult())
                    .estimatedTax(t.estimatedTax()).resultAfterTax(t.afterTax()).hasRecords(month.hasRecords())
                    .inProgress(month.inProgress()).future(month.future()).build());
        }
        return YearlyFinancialReportDto.builder().months(rows).yearlyTotalRevenue(data.summary().revenue())
                .yearlyTotalCosts(legacyCosts(data.summary())).metrics(data.summary()).basis(basis).asOf(data.asOf())
                .firstMonth(data.firstMonth()).apartments(data.apartments()).timeline(data.months()).build();
    }

    public MonthlyFinancialDetailDto getMonthlyFinancialDetail(LocalDate month) { return getMonthlyFinancialDetail(month,null,Basis.ACCRUAL); }
    public MonthlyFinancialDetailDto getMonthlyFinancialDetail(LocalDate month,Long apartmentId,Basis basis) {
        month=month.withDayOfMonth(1);
        var data=analytics.report(month,month,apartmentId,basis,true);
        var year=analytics.report(month.withMonth(1),month,apartmentId,basis,true);
        List<ApartmentFinancialDetailDto> apartments=new ArrayList<>();
        for(var property:data.properties()) {
            if(property.months().stream().noneMatch(Month::hasRecords)) continue;
            var t=property.totals();
            apartments.add(ApartmentFinancialDetailDto.builder().apartmentId(property.id()).apartmentName(property.name())
                    .administrativeFee(category(property.costs(),"ADMINISTRATIVE_FEE")).internet(category(property.costs(),"INTERNET"))
                    .electricity(category(property.costs(),"ELECTRICITY")).gas(category(property.costs(),"GAS"))
                    .water(category(property.costs(),"WATER")).other(category(property.costs(),"OTHER"))
                    .tax(t.estimatedTax()).revenue(t.revenue()).costs(legacyCosts(t)).netIncome(afterTax(t))
                    .operatingCosts(t.costs()).operatingResult(t.result()).estimatedTax(t.estimatedTax()).resultAfterTax(t.afterTax()).build());
        }
        var t=data.summary();
        var summary=MonthlyFinancialSummaryDto.builder().totalAdministrativeFee(category(data.costs(),"ADMINISTRATIVE_FEE"))
                .totalInternet(category(data.costs(),"INTERNET")).totalElectricity(category(data.costs(),"ELECTRICITY"))
                .totalGas(category(data.costs(),"GAS")).totalWater(category(data.costs(),"WATER")).totalOther(category(data.costs(),"OTHER"))
                .totalTax(t.estimatedTax()).totalRevenue(t.revenue()).totalCosts(legacyCosts(t)).netIncome(afterTax(t))
                .operatingCosts(t.costs()).operatingResult(t.result()).estimatedTax(t.estimatedTax()).resultAfterTax(t.afterTax())
                .cumulativeNetIncome(afterTax(year.summary())).cumulativeOperatingResult(year.summary().result()).build();
        return MonthlyFinancialDetailDto.builder().month(month).summary(summary).apartments(apartments).basis(basis).asOf(data.asOf()).build();
    }
    private BigDecimal category(List<Category> categories,String key) { return categories.stream().filter(c->c.key().equals(key)).map(Category::amount).findFirst().orElse(ZERO); }
    private BigDecimal legacyCosts(Totals t) { return t.costs().add(t.estimatedTax()==null?ZERO:t.estimatedTax()); }
    private BigDecimal afterTax(Totals t) { return t.afterTax()==null?t.result():t.afterTax(); }
}
