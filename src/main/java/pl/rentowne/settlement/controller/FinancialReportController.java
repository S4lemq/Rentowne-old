package pl.rentowne.settlement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.settlement.model.dto.MonthlyFinancialDetailDto;
import pl.rentowne.settlement.model.dto.MonthlyFinancialReportDto;
import pl.rentowne.settlement.service.FinancialReportService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Raporty finansowe")
public class FinancialReportController {

    private final FinancialReportService financialReportService;

    @Operation(
            summary = "Pobiera miesięczny raport finansowy",
            description = "Metoda służy do pobrania raportu finansowego z podziałem na miesiące. " +
                    "Raport zawiera przychody z rozliczeń, koszty z faktur oraz podatek od odstępnego (8.5%), " +
                    "dochód netto oraz sumę narastającą dochodu. Możliwe jest filtrowanie po roku.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Raport został wygenerowany pomyślnie", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/financial-reports/monthly")
    public ResponseEntity<List<MonthlyFinancialReportDto>> getMonthlyFinancialReport(
            @RequestParam(required = false) Integer year) {
        return new ResponseEntity<>(financialReportService.getMonthlyFinancialReport(year), HttpStatus.OK);
    }

    @Operation(
            summary = "Pobiera szczegóły finansowe dla konkretnego miesiąca",
            description = "Metoda służy do pobrania szczegółowego raportu finansowego dla wybranego miesiąca. " +
                    "Raport zawiera podział na mieszkania z kwotami faktur według typów (opłaty administracyjne, " +
                    "internet, prąd, gaz), podatek od odstępnego (8.5%), przychody, koszty oraz dochód netto. " +
                    "Dodatkowo zwracane jest podsumowanie dla całego miesiąca.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Szczegóły miesiąca zostały pobrane pomyślnie", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/financial-reports/monthly/details")
    public ResponseEntity<MonthlyFinancialDetailDto> getMonthlyFinancialDetail(
            @RequestParam LocalDate month) {
        return new ResponseEntity<>(financialReportService.getMonthlyFinancialDetail(month), HttpStatus.OK);
    }
}
