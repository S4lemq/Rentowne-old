package pl.rentowne.meter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.common.controler.AbstractController;
import pl.rentowne.meter.model.dto.MeterDto;
import pl.rentowne.meter.model.dto.MeterListDto;
import pl.rentowne.meter.service.MeterService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Liczniki")
public class MeterController extends AbstractController {

    private final MeterService meterService;

    @Operation(
            summary = "Zapisuje licznik dla danego obiektu do wynajęcia",
            description = "Metoda służy do zapisu licznika dla danego obiektu do wynajecia",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Licznik dodano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PostMapping("/api/meter")
    public ResponseEntity<MeterDto> addMeter(@Parameter(description = "dto licznika", required = true) @RequestBody MeterDto meter) {
        return new ResponseEntity<>(meterService.addMeter(meter), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Aktualizuje dane mieszkania",
            description = "Metoda służy do aktualizowania danych mieszkania",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dane licznika zaktualizowano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PutMapping("/api/meter")
    public ResponseEntity<Void> updateMeter(@Parameter(description = "dto licznika", required = true) @RequestBody MeterDto meter) {
        meterService.updateMeter(meter);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Pobiera licznik po id",
            description = "Metoda służy do pobrania licznika po id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Licznik pobrano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/meter/{id}")
    public ResponseEntity<MeterDto> getMeter(@Parameter(description = "id licznika", required = true) @PathVariable Long id) {
        return new ResponseEntity<>(meterService.getMeterById(id), HttpStatus.OK);
    }

    @Operation(
            summary = "Pobiera liczbę liczników danego obiektu",
            description = "Metoda służy do pobrania liczby liczników po id obiektu",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dane pobrano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/rented-object/{id}/meter-count")
    public ResponseEntity<Void> getMeterCountByRentedObject(@PathVariable Long id) {
        meterService.getMeterCountByRentedObject(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Pobiera paginowaną listę liczników",
            description = "Metoda służy do pobrania listy liczników zalogowanego użytkownika z możliwością paginacji, sortowania po wszystkich kolumnach oraz filtrowania po ID mieszkania i nazwie mieszkania/obiektu",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista liczników została pobrana pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/meters")
    public ResponseEntity<Page<MeterListDto>> getMetersList(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @Parameter(description = "ID mieszkania do filtrowania") @RequestParam(required = false) Long apartmentId,
            @Parameter(description = "Tekst do wyszukiwania w nazwie mieszkania lub obiektu") @RequestParam(required = false) String searchText) {
        return new ResponseEntity<>(meterService.getMetersList(pageable, apartmentId, searchText), HttpStatus.OK);
    }
}
