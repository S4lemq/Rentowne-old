package pl.rentowne.settlement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.settlement.CalculateRequestDto;
import pl.rentowne.settlement.model.dto.SettlementListDto;
import pl.rentowne.settlement.service.SettlementService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Rozliczenia")
public class SettlementController {

    private final SettlementService settlementService;

    @DeleteMapping(value = "/api/settlements/{id}")
    public ResponseEntity<Void> deleteCalculate(@PathVariable Long id) {
        settlementService.deleteCalculate(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(
            summary = "Pobiera paginowaną listę rozliczeń",
            description = "Metoda służy do pobrania listy rozliczeń dla danego obiektu do wynajęcia i najemcy z możliwością paginacji i sortowania po wszystkich kolumnach. Domyślne sortowanie: settlementDate DESC",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista rozliczeń została pobrana pomyślnie", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/settlements")
    public ResponseEntity<Page<SettlementListDto>> getSettlements(
            @PageableDefault(size = 20, sort = "settlementDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam Long rentedObjectId,
            @RequestParam Long tenantId) {
        return new ResponseEntity<>(settlementService.getSettlements(pageable, rentedObjectId, tenantId), HttpStatus.OK);
    }

    @Operation(
            summary = "Uruchamia kalkulację rozliczenia dla obiektu",
            description = "Wywołuje usługę rozliczeń dla wskazanego obiektu na podstawie danych przesłanych w CalculateRequestDto"
    )
    @PostMapping(value = "/api/rented-objects/{id}/calculate")
    ResponseEntity<Void> calculate(@PathVariable final Long id, @RequestBody final CalculateRequestDto dto) {
        settlementService.calculate(id, dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(
            summary = "Sprawdza czy najemca ma jakieś rozliczenia",
            description = "Metoda sprawdza czy dla danego najemcy istnieją jakiekolwiek rozliczenia (Settlement)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sprawdzenie powiodło się", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/settlements/tenant/{tenantId}/exists")
    public ResponseEntity<Boolean> existsSettlementsByTenant(@PathVariable Long tenantId) {
        return new ResponseEntity<>(settlementService.existsSettlementsByTenantId(tenantId), HttpStatus.OK);
    }
}
