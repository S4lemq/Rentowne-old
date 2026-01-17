package pl.rentowne.meter_reading.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.common.controler.AbstractController;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.meter_reading.model.dto.MeterReadingDto;
import pl.rentowne.meter_reading.model.dto.MeterReadingRowDto;
import pl.rentowne.meter_reading.model.dto.PrevMeterReadingDto;
import pl.rentowne.meter_reading.repository.MeterReadingRepository;
import pl.rentowne.meter_reading.service.MeterReadingService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Odczyty liczników")
public class MeterReadingController extends AbstractController {

    private final MeterReadingService meterReadingService;
    private final MeterReadingRepository meterReadingRepository;

    @Operation(
            summary = "Zapisuje stan licznika",
            description = "Metoda służy do zapisu stanu licznika",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Stan licznik dodano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PostMapping("/api/meter-reading")
    public ResponseEntity<Void> addMeterReading(@RequestBody MeterReadingDto dto) throws RentowneBusinessException {
        meterReadingService.addMeterReading(dto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }


    @Operation(
            summary = "Pobiera aktualny stan licznika po id licznika",
            description = "Metoda służy do pobrania aktualnego stanu licznika po id licznika",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Stan licznika pobrano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/meter/{meterId}/prev-meter-reading")
    public ResponseEntity<PrevMeterReadingDto> getMeterReadingByMeter(@PathVariable Long meterId) {
        return ResponseEntity.ok(meterReadingService.getMeterReadingByMeter(meterId));
    }


    @Operation(
            summary = "Pobiera aktualny oraz poprzedni stan licznika po id licznika oraz id stanu licznika",
            description = "Metoda służy do pobrania aktualnego oraz poprzedniego stanu licznika po id licznika oraz id stanu licznika",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Stan licznika pobrano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/meter/{meterId}/meter-reading/{id}")
    public ResponseEntity<MeterReadingDto> getCurrentAndPreviousReading(@PathVariable Long meterId, @PathVariable Long id) {
        return ResponseEntity.ok(meterReadingService.getCurrentAndPreviousReading(meterId, id));
    }

    @Operation(
            summary = "Usuwa odczyt licznika",
            description = "Metoda służy do usunięcia odczytu licznika po id",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Odczyt licznika usunięto pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono odczytu licznika"),
                    @ApiResponse(responseCode = "409", description = "Odczyt licznika jest już rozliczony"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @DeleteMapping("/api/meter-reading/{id}")
    public ResponseEntity<Void> deleteMeterReading(@PathVariable Long id) throws RentowneBusinessException {
        meterReadingService.deleteMeterReading(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "Pobiera listę odczytów liczników z paginacją",
            description = "Metoda zwracająca stronicowaną listę odczytów dla konkretnego licznika",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sukces"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/meter-readings/list")
    public ResponseEntity<Page<MeterReadingRowDto>> getMeterReadingsList(
            Pageable pageable,
            @RequestParam Long meterId
    ) {
        Page<MeterReadingRowDto> meterReadings = meterReadingRepository.getMeterReadingsList(pageable, meterId);
        return ResponseEntity.ok(meterReadings);
    }
}
