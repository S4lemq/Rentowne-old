package pl.rentowne.meter;

import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Liczniki")
class MeterController extends AbstractController {

    private final MeterService meterService;

    @Operation(
        summary = "Zapisuje licznik dla danego obiektu do wynajęcia",
        description = "Metoda służy do zapisu licznika dla danego obiektu do wynajecia"
    )
    @PostMapping("/api/meter")
    ResponseEntity<MeterDto> addMeter(@RequestBody final MeterDto meter) {
        return new ResponseEntity<>(meterService.addMeter(meter), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Zapisuje licznik dla danego mieszkania",
        description = "Metoda służy do zapisu licznika przypisanego bezpośrednio do mieszkania"
    )
    @PostMapping("/api/apartment-meter")
    ResponseEntity<ApartmentMeterDto> addApartmentMeter(@RequestBody final ApartmentMeterDto meter) {
        return new ResponseEntity<>(meterService.addApartmentMeter(meter), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Aktualizuje dane mieszkania",
        description = "Metoda służy do aktualizowania danych mieszkania"
    )
    @PutMapping("/api/meter")
    ResponseEntity<Void> updateMeter(@RequestBody final MeterDto meter) {
        meterService.updateMeter(meter);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Aktualizuje licznik przypisany do mieszkania",
        description = "Metoda służy do aktualizowania licznika przypisanego bezpośrednio do mieszkania"
    )
    @PutMapping("/api/apartment-meter")
    ResponseEntity<Void> updateApartmentMeter(@RequestBody final ApartmentMeterDto meter) {
        meterService.updateApartmentMeter(meter);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Pobiera licznik po id",
        description = "Metoda służy do pobrania licznika po id"
    )
    @GetMapping("/api/meter/{id}")
    ResponseEntity<MeterDto> getMeter(@PathVariable final Long id) {
        return new ResponseEntity<>(meterService.getMeterById(id), HttpStatus.OK);
    }

    @Operation(
        summary = "Pobiera liczbę liczników danego obiektu",
        description = "Metoda służy do pobrania liczby liczników po id obiektu"
    )
    @GetMapping("/api/rented-object/{id}/meter-count")
    ResponseEntity<Void> getMeterCountByRentedObject(@PathVariable final Long id) {
        meterService.getMeterCountByRentedObject(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Pobiera paginowaną listę liczników",
        description = "Metoda służy do pobrania listy liczników zalogowanego użytkownika z możliwością paginacji, sortowania po wszystkich kolumnach oraz filtrowania po ID mieszkania, typie dostawcy i nazwie mieszkania/obiektu"
    )
    @GetMapping("/api/meters")
    ResponseEntity<Page<MeterListDto>> getMetersList(@PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) final Pageable pageable,
                                                     @RequestParam(required = false) final Long apartmentId,
                                                     @RequestParam(required = false) final MeterType meterType,
                                                     @RequestParam(required = false) final String searchText) {
        return new ResponseEntity<>(meterService.getMetersList(pageable, apartmentId, meterType, searchText), HttpStatus.OK);
    }

    @Operation(
        summary = "Pobiera liczniki przypisane do mieszkania",
        description = "Metoda służy do pobrania listy liczników przypisanych bezpośrednio do mieszkania"
    )
    @GetMapping("/api/apartment-meters")
    ResponseEntity<List<ApartmentMeterDto>> getApartmentMeters(@RequestParam final Long apartmentId) {
        return new ResponseEntity<>(meterService.getApartmentMeters(apartmentId), HttpStatus.OK);
    }
}
