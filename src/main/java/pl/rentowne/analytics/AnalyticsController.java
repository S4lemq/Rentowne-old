package pl.rentowne.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import static pl.rentowne.analytics.AnalyticsDto.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
class AnalyticsController {
    private final FinancialAnalyticsService analytics;
    private final ForecastService forecast;

    @GetMapping("/overview")
    ResponseEntity<Overview> overview(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
                                     @RequestParam(required=false) Long apartmentId, @RequestParam(defaultValue="ACCRUAL") Basis basis) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(analytics.overview(from,to,apartmentId,basis));
    }

    @GetMapping("/forecast")
    ResponseEntity<Forecast> forecast(@RequestParam(defaultValue="6") int months, @RequestParam(required=false) Long apartmentId,
                                     @RequestParam(defaultValue="false") boolean continueTenancy,
                                     @RequestParam(defaultValue="0") BigDecimal costChangePercent) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(forecast.forecast(months,apartmentId,continueTenancy,costChangePercent));
    }
}
