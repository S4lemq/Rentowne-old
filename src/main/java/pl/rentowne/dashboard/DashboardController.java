package pl.rentowne.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pulpit")
class DashboardController {
    private final DashboardService service;

    @GetMapping("/api/dashboard")
    @Operation(summary = "Podsumowanie najmu zalogowanego właściciela",
            description = "Wpłaty bez kaucji i opłacone faktury według dat płatności; bieżące salda i terminy. Zakres: 6 lub 12 miesięcy.")
    public ResponseEntity<DashboardDto> getDashboard(@RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getDashboard(months));
    }
}
