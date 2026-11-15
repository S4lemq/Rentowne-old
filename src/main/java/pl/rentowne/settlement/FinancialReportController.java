package pl.rentowne.settlement;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.analytics.AnalyticsDto.Basis;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "Raporty finansowe")
class FinancialReportController {
    private final FinancialReportService service;

    @GetMapping("/api/financial-reports/monthly")
    YearlyFinancialReportDto monthly(@RequestParam(required=false) Integer year,
                                     @RequestParam(required=false) Long apartmentId,
                                     @RequestParam(defaultValue="ACCRUAL") Basis basis) {
        return service.getMonthlyFinancialReport(year,apartmentId,basis);
    }
    @GetMapping("/api/financial-reports/monthly/details")
    MonthlyFinancialDetailDto details(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate month,
                                      @RequestParam(required=false) Long apartmentId,
                                      @RequestParam(defaultValue="ACCRUAL") Basis basis) {
        return service.getMonthlyFinancialDetail(month,apartmentId,basis);
    }
}
