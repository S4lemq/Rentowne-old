package pl.rentowne.settlement;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@Tag(name = "Rozliczenia")
class SettlementController {

    private static final MediaType EXCEL_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final SettlementService settlementService;
    private final SettlementExcelExporter settlementExcelExporter;

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
            summary = "Eksportuje wszystkie rozliczenia do pliku Excel",
            description = "Eksport zwraca wszystkie rozliczenia bez filtrowania."
    )
    @GetMapping(value = "/api/settlements/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<ByteArrayResource> exportSettlementsToExcel() {
        byte[] content = settlementExcelExporter.export(settlementService.getAllSettlements());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(EXCEL_MEDIA_TYPE);
        headers.setContentLength(content.length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("settlements.xlsx", StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl(CacheControl.noStore().getHeaderValue());

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(content));
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
