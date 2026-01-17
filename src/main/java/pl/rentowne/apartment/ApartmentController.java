package pl.rentowne.apartment;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.common.controler.AbstractController;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneNotFoundException;
import pl.rentowne.rented_object.model.dto.RentedObjectDto;
import pl.rentowne.rented_object.service.RentedObjectService;
import pl.rentowne.user.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mieszkania")
class ApartmentController extends AbstractController {

    private final ApartmentService apartmentService;
    private final RentedObjectService rentedObjectService;
    private final ApartmentRepository apartmentRepository;
    private final UserService userService;

    @Operation(
        summary = "Pobiera mieszkanie",
        description = "Metoda zwracająca mieszkanie po wskazanym id"
    )
    @GetMapping(value = "/api/apartments/{id}")
    ResponseEntity<ApartmentDto> getApartment(@PathVariable final Long id) throws RentowneNotFoundException {
        return ResponseEntity.ok(apartmentService.getApartment(id));
    }

    @Operation(
        summary = "Zapisuje mieszkanie",
        description = "Metoda służy do zapisu mieszkania"
    )
    @PostMapping("/api/apartments")
    ResponseEntity<ApartmentDto> addApartment(@RequestBody final ApartmentDto apartmentDto) throws RentowneBusinessException {
        final Long apartmentId = apartmentService.addApartment(apartmentDto);
        return new ResponseEntity<>(apartmentService.getApartment(apartmentId), HttpStatus.CREATED);
    }

    @Operation(
        summary = "Aktualizuje dane mieszkania",
        description = "Metoda służy do aktualizowania danych mieszkania"
    )
    @PutMapping("/api/apartments/{id}")
    ResponseEntity<ApartmentDto> updateApartment(@RequestBody final ApartmentDto apartmentDto, @PathVariable final Long id) throws RentowneBusinessException {
        apartmentService.updateApartment(apartmentDto, id);
        return ResponseEntity.ok(apartmentService.getApartment(id));
    }

    @Operation(
            summary = "Usuwa mieszkanie",
            description = "Metoda służy do usunięcia mieszkania"
    )
    @DeleteMapping("/api/apartments/{id}")
    ResponseEntity<Void> deleteApartment(@PathVariable final Long id) {
        apartmentService.deleteApartment(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(
        summary = "Pobiera obiekty do wynajęcia wraz z licznikami",
        description = "Metoda zwracająca obiekty do wynajęcia z licznikami po id mieszkania"

    )
    @GetMapping(value = "/api/apartments/{id}/rented-objects/meters")
    ResponseEntity<List<RentedObjectDto>> getRentedObjectsWithMeters(@PathVariable final Long id) {
        return ResponseEntity.ok(rentedObjectService.getAllRentedObjectDtosWithMeters(id));
    }

    @GetMapping(value = "/api/apartments/all-by-logged-user")
    ResponseEntity<List<ApartmentDto>> getAllApartmentsByLoggedUser(@RequestParam(name = "apartmentId", required = false) final Long apartmentId) throws RentowneBusinessException {
        return ResponseEntity.ok(apartmentService.getAllApartmentsByLoggedUserAndApartment(apartmentId));
    }

    @Operation(
        summary = "Pobiera wszystkie mieszkania zalogowanego użytkownika",
        description = "Metoda zwracająca wszystkie mieszkania należące do zalogowanego użytkownika"
    )
    @GetMapping(value = "/api/apartments/my-apartments")
    ResponseEntity<List<ApartmentDto>> getMyApartments() throws RentowneBusinessException {
        return ResponseEntity.ok(apartmentService.getAllApartmentsByLoggedUser());
    }

    @Operation(
        summary = "Przypisuje dostawcę do mieszkania",
        description = "Metoda służy do zapisu mieszkania"
    )
    @PostMapping("/api/apartments/add-housing-provider")
    ResponseEntity<Void> addHousingProvider(@RequestBody final ApartmentHousingProviderRequest dto) throws RentowneNotFoundException {
        this.apartmentService.addHousingProviders(dto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Operation(
        summary = "Służy do przypinania nieruchomości",
        description = "Metoda służy do przypinania nieruchomości"
    )
    @PostMapping("/api/apartments/{id}/pin")
    ResponseEntity<Void> pinApartment(@RequestParam final boolean isPinned, @PathVariable final long id) {
        apartmentService.pinApartment(isPinned, id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(
        summary = "Pobiera listę mieszkań z paginacją",
        description = "Metoda zwracająca stronicowaną listę mieszkań zalogowanego użytkownika z możliwością filtrowania"
    )
    @GetMapping("/api/apartments/list")
    ResponseEntity<Page<ApartmentDto>> getApartmentsList(final Pageable pageable,
                                                         @RequestParam(required = false) final String apartmentName,
                                                         @RequestParam(required = false) final Boolean isRented,
                                                         @RequestParam(required = false) final String searchText) throws RentowneBusinessException {
        final Long userId = userService.getLoggedUser().getId();
        final Page<ApartmentDto> apartments = apartmentRepository.getApartmentsList(
                pageable, userId, apartmentName, isRented, searchText
        );
        return ResponseEntity.ok(apartments);
    }

}
