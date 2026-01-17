package pl.rentowne.rented_object.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.common.controler.AbstractController;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.rented_object.model.dto.CalculateRequestDto;
import pl.rentowne.rented_object.model.dto.RentedObjectDetailsDto;
import pl.rentowne.rented_object.model.dto.RentedObjectDto;
import pl.rentowne.rented_object.model.dto.RentedObjectSettlementRowDto;
import pl.rentowne.rented_object.repository.RentedObjectRepository;
import pl.rentowne.rented_object.service.RentedObjectService;
import pl.rentowne.settlement.service.SettlementService;
import pl.rentowne.user.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Obiekty do wynajęcia")
public class RentedObjectController extends AbstractController {

    private final RentedObjectService rentedObjectService;
    private final SettlementService settlementService;
    private final RentedObjectRepository rentedObjectRepository;
    private final UserService userService;

    @Operation(
            summary = "Pobiera wszystkie obiekty do wynajęcia dla zalogowanego użytkownika",
            description = "Metoda zwracająca obiekty do wynajęcia dla zalogowanego użytkownika",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sukces", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping(value = "/api/rented-objects/all")
    public ResponseEntity<List<RentedObjectDto>> getAllRentedObjects() throws RentowneBusinessException {
        return ResponseEntity.ok(rentedObjectService.getAllRentedObjectDtos());
    }

    @GetMapping(value = "/api/apartments/{id}/rented-objects/all")
    public ResponseEntity<List<RentedObjectDto>> getAllRentedObjectsByApartmentAndAdditionalRentedObject(
            @PathVariable Long id,
            @RequestParam(name = "rentedObjectId", required = false) Long rentedObjectId) throws RentowneBusinessException {
        return ResponseEntity.ok(rentedObjectService.getAllByApartmentAndOptionalRentedObject(id, rentedObjectId));
    }

    @Operation(
            summary = "Pobiera szczegółowe informacje o obiektach z rozliczeniami",
            description = "Metoda zwracająca szczegółowe informacje o obiektach do wynajęcia wraz z danymi z ostatnich rozliczeń",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sukces", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping(value = "/api/rented-objects/details")
    public ResponseEntity<List<RentedObjectDetailsDto>> getRentedObjectsDetails(
            @RequestParam(name = "apartmentId", required = false) Long apartmentId,
            @RequestParam(name = "active") Boolean active) {
        return ResponseEntity.ok(rentedObjectService.getRentedObjectsDetails(apartmentId, active));
    }

    @Operation(
            summary = "Sprawdza czy obiekt posiada gasDeposit",
            description = "Metoda sprawdzająca czy dany obiekt do wynajęcia ma gasDeposit w ostatnim rozliczeniu",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sukces", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono obiektu"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping(value = "/api/rented-objects/{id}/has-gas-deposit")
    public ResponseEntity<Boolean> hasGasDeposit(@PathVariable Long id) {
        return ResponseEntity.ok(rentedObjectService.hasGasDeposit(id));
    }

    @PostMapping(value = "/api/rented-objects/{id}/calculate")
    public ResponseEntity<Void> calculate(@PathVariable Long id, @RequestBody CalculateRequestDto dto) {
        settlementService.calculate(id, dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(
            summary = "Sprawdza czy należy zasugerować rozliczenie niepełnego miesiąca",
            description = "Metoda sprawdzająca czy dla wybranej daty rozliczenia należy zasugerować użytkownikowi rozliczenie niepełnego miesiąca na podstawie daty rozpoczęcia umowy najmu",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sukces", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono obiektu lub umowy najmu"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping(value = "/api/rented-objects/{rentedObjectId}/check-proration")
    public ResponseEntity<pl.rentowne.rented_object.model.dto.ProrationCheckResponseDto> checkProrationSuggestion(
            @PathVariable Long rentedObjectId,
            @RequestParam String settlementDate) {
        return ResponseEntity.ok(rentedObjectService.checkProrationSuggestion(rentedObjectId, settlementDate));
    }

    @Operation(
            summary = "Pobiera listę obiektów do wynajęcia z rozliczeniami (paginowana)",
            description = "Metoda zwracająca stronicowaną listę obiektów do wynajęcia zalogowanego użytkownika z możliwością wyszukiwania",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sukces"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/rented-objects/settlements-list")
    public ResponseEntity<Page<RentedObjectSettlementRowDto>> getRentedObjectSettlementsList(
            Pageable pageable,
            @RequestParam(required = false) String searchText
    ) throws RentowneBusinessException {
        Long userId = userService.getLoggedUser().getId();
        Page<RentedObjectSettlementRowDto> rentedObjects = rentedObjectRepository.getRentedObjectSettlementsList(
                pageable, userId, searchText
        );
        return ResponseEntity.ok(rentedObjects);
    }
}
