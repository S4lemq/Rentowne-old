package pl.rentowne.housing_provider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import pl.rentowne.exception.RentowneBusinessException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Dostawcy świadczeń")
class HousingProviderController extends AbstractController {

    private final HousingProviderService housingProviderService;

    @Operation(
        summary = "Zapisuje dostawcę świadczeń",
        description = "Metoda służy do zapisu dostawcy świadczeń"
    )
    @PostMapping("/api/housing-service-provider")
    ResponseEntity<Long> addHousingServiceProvider(@RequestBody final HousingProviderDto dto) throws RentowneBusinessException {
        return new ResponseEntity<>(housingProviderService.addHousingServiceProvider(dto), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Pobiera dostawcę po id",
        description = "Metoda służy do pobrania dostawcy po id"
    )
    @GetMapping("/api/housing-service-provider/{id}")
    ResponseEntity<HousingProviderDto> getHousingServiceProvider(@PathVariable final Long id) {
        return new ResponseEntity<>(housingProviderService.getHousingProviderById(id), HttpStatus.OK);
    }

    @Operation(
        summary = "Aktualizuje dane dostawcy",
        description = "Metoda służy do aktualizowania danych dostawcy"
    )
    @PutMapping("/api/housing-service-provider")
    ResponseEntity<Void> updateHousingProvider(@RequestBody final HousingProviderDto dto) {
        housingProviderService.updateHousingProvider(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Pobiera wszystkich dostawców dla zalogowanego użytkownika",
        description = "Metoda służy do pobrania listy wszystkich dostawców przypisanych do zalogowanego użytkownika"
    )
    @GetMapping("/api/housing-service-provider/user")
    ResponseEntity<List<HousingProviderBasicDto>> getHousingProvidersForLoggedUser() {
        return new ResponseEntity<>(housingProviderService.getHousingProvidersForLoggedUser(), HttpStatus.OK);
    }

    @Operation(
        summary = "Pobiera paginowaną listę dostawców świadczeń",
        description = "Metoda służy do pobrania listy dostawców świadczeń zalogowanego użytkownika z możliwością paginacji, sortowania po wszystkich kolumnach oraz filtrowania po ID mieszkania i nazwie dostawcy/mieszkania"
    )
    @GetMapping("/api/housing-service-providers")
    ResponseEntity<Page<HousingProviderListDto>> getHousingProvidersList(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) final Pageable pageable,
            @Parameter(description = "ID mieszkania do filtrowania") @RequestParam(required = false) final Long apartmentId,
            @Parameter(description = "Tekst do wyszukiwania w nazwie dostawcy lub mieszkania") @RequestParam(required = false) final String searchText) {
        return new ResponseEntity<>(housingProviderService.getHousingProvidersList(pageable, apartmentId, searchText), HttpStatus.OK);
    }

    @Operation(
        summary = "Pobiera dostawców świadczeń dla konkretnego mieszkania",
        description = "Metoda służy do pobrania paginowanej listy dostawców świadczeń przypisanych do wybranego mieszkania zalogowanego użytkownika"
    )
    @GetMapping("/api/apartments/{apartmentId}/housing-providers")
    ResponseEntity<Page<HousingProviderApartmentDto>> getHousingProvidersForApartment(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) final Pageable pageable,
            @Parameter(description = "ID mieszkania", required = true) @PathVariable final Long apartmentId) {
        return new ResponseEntity<>(housingProviderService.getHousingProvidersForApartment(pageable, apartmentId), HttpStatus.OK);
    }

    @Operation(
        summary = "Pobiera wszystkich dostawców z informacją o przypisaniu do mieszkania",
        description = "Metoda służy do pobrania paginowanej listy wszystkich dostawców użytkownika z flagą określającą czy są przypisani do wybranego mieszkania. Używane w oknie wyboru dostawców."
    )
    @GetMapping("/api/apartments/{apartmentId}/housing-providers/all-with-assignment")
    ResponseEntity<Page<HousingProviderWithAssignmentDto>> getAllProvidersWithAssignmentStatus(
            @PageableDefault(size = 100, sort = "name", direction = Sort.Direction.ASC) final Pageable pageable,
            @Parameter(description = "ID mieszkania", required = true) @PathVariable final Long apartmentId) {
        return new ResponseEntity<>(housingProviderService.getAllProvidersWithAssignmentStatus(pageable, apartmentId), HttpStatus.OK);
    }

}
