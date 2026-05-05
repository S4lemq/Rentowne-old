package pl.rentowne.rented_object;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.common.controler.AbstractController;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.user.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Obiekty do wynajęcia")
class RentedObjectController extends AbstractController {

    private final RentedObjectService rentedObjectService;
    private final RentedObjectRepository rentedObjectRepository;
    private final UserService userService;

    @Operation(
        summary = "Pobiera wszystkie obiekty do wynajęcia dla zalogowanego użytkownika",
        description = "Metoda zwracająca obiekty do wynajęcia dla zalogowanego użytkownika"
    )
    @GetMapping(value = "/api/rented-objects/all")
    ResponseEntity<List<RentedObjectDto>> getAllRentedObjects() throws RentowneBusinessException {
        return ResponseEntity.ok(rentedObjectService.getAllRentedObjectDtos());
    }

    @Operation(
        summary = "Pobiera obiekty do wynajęcia dla mieszkania (opcjonalnie z dodatkowym obiektem)",
        description = "Zwraca listę obiektów do wynajęcia powiązanych z danym mieszkaniem; opcjonalny parametr rentedObjectId pozwala uwzględnić dodatkowy obiekt w wynikach"
    )
    @GetMapping(value = "/api/apartments/{id}/rented-objects/all")
    ResponseEntity<List<RentedObjectDto>> getAllRentedObjectsByApartmentAndAdditionalRentedObject(@PathVariable final Long id,
                                                                                                   @RequestParam(name = "rentedObjectId", required = false) final Long rentedObjectId) throws RentowneBusinessException {
        return ResponseEntity.ok(rentedObjectService.getAllByApartmentAndOptionalRentedObject(id, rentedObjectId));
    }

    @Operation(
        summary = "Pobiera szczegółowe informacje o obiektach z rozliczeniami",
        description = "Metoda zwracająca szczegółowe informacje o obiektach do wynajęcia wraz z danymi z ostatnich rozliczeń"
    )
    @GetMapping(value = "/api/rented-objects/details")
    ResponseEntity<List<RentedObjectDetailsDto>> getRentedObjectsDetails(@RequestParam(name = "apartmentId", required = false) final Long apartmentId,
                                                                         @RequestParam(name = "active") final Boolean active) {
        return ResponseEntity.ok(rentedObjectService.getRentedObjectsDetails(apartmentId, active));
    }

    @Operation(
            summary = "Pobiera obiekty do wynajęcia wraz z licznikami",
            description = "Metoda zwracająca obiekty do wynajęcia z licznikami po id mieszkania"

    )
    @GetMapping(value = "/api/apartments/{id}/rented-objects/meters")
    ResponseEntity<List<RentedObjectDto>> getRentedObjectsWithMeters(@PathVariable final Long id) {
        return ResponseEntity.ok(rentedObjectService.getAllRentedObjectDtosWithMeters(id));
    }

    @Operation(
        summary = "Sprawdza czy obiekt posiada gasDeposit",
        description = "Metoda sprawdzająca czy dany obiekt do wynajęcia ma gasDeposit w ostatnim rozliczeniu"
    )
    @GetMapping(value = "/api/rented-objects/{id}/has-gas-deposit")
    ResponseEntity<Boolean> hasGasDeposit(@PathVariable final Long id) {
        return ResponseEntity.ok(rentedObjectService.hasGasDeposit(id));
    }

    @Operation(
        summary = "Sprawdza czy należy zasugerować rozliczenie niepełnego miesiąca",
        description = "Metoda sprawdzająca czy dla wybranej daty rozliczenia należy zasugerować użytkownikowi rozliczenie niepełnego miesiąca na podstawie daty rozpoczęcia umowy najmu"
    )
    @GetMapping(value = "/api/rented-objects/{rentedObjectId}/check-proration")
    ResponseEntity<ProrationCheckResponseDto> checkProrationSuggestion(@PathVariable final Long rentedObjectId,
                                                                       @RequestParam final String settlementDate) {
        return ResponseEntity.ok(rentedObjectService.checkProrationSuggestion(rentedObjectId, settlementDate));
    }

    @Operation(
        summary = "Pobiera listę obiektów do wynajęcia z rozliczeniami (paginowana)",
        description = "Metoda zwracająca stronicowaną listę obiektów do wynajęcia zalogowanego użytkownika z możliwością wyszukiwania"
    )
    @GetMapping("/api/rented-objects/settlements-list")
    ResponseEntity<Page<RentedObjectSettlementRowDto>> getRentedObjectSettlementsList(final Pageable pageable,
                                                                                      @RequestParam(required = false) final String searchText) throws RentowneBusinessException {
        final Long userId = userService.getLoggedUser().id();
        final Page<RentedObjectSettlementRowDto> rentedObjects = rentedObjectRepository.getRentedObjectSettlementsList(
                pageable, userId, searchText
        );
        return ResponseEntity.ok(rentedObjects);
    }
}
