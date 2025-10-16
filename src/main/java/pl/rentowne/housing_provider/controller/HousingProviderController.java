package pl.rentowne.housing_provider.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.common.controler.AbstractController;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.housing_provider.model.dto.HousingProviderBasicDto;
import pl.rentowne.housing_provider.model.dto.HousingProviderDto;
import pl.rentowne.housing_provider.service.HousingProviderService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Dostawcy świadczeń")
public class HousingProviderController extends AbstractController {

    private final HousingProviderService housingProviderService;

    @Operation(
            summary = "Zapisuje dostawcę świadczeń",
            description = "Metoda służy do zapisu dostawcy świadczeń",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Dostawcę świadczeń dodano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PostMapping("/api/housing-service-provider")
    public ResponseEntity<Long> addHousingServiceProvider(@RequestBody HousingProviderDto dto) throws RentowneBusinessException {
        return new ResponseEntity<>(housingProviderService.addHousingServiceProvider(dto), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Pobiera dostawcę po id",
            description = "Metoda służy do pobrania dostawcy po id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dostawcę pobrano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/housing-service-provider/{id}")
    public ResponseEntity<HousingProviderDto> getHousingServiceProvider(@PathVariable Long id) {
        return new ResponseEntity<>(housingProviderService.getHousingProviderById(id), HttpStatus.OK);
    }

    @Operation(
            summary = "Aktualizuje dane dostawcy",
            description = "Metoda służy do aktualizowania danych dostawcy",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dane dostawcy zaktualizowano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PutMapping("/api/housing-service-provider")
    public ResponseEntity<Void> updateHousingProvider(@RequestBody HousingProviderDto dto) {
        housingProviderService.updateHousingProvider(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Pobiera wszystkich dostawców dla zalogowanego użytkownika",
            description = "Metoda służy do pobrania listy wszystkich dostawców przypisanych do zalogowanego użytkownika",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista dostawców została pobrana pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/housing-service-provider/user")
    public ResponseEntity<List<HousingProviderBasicDto>> getHousingProvidersForLoggedUser() {
        return new ResponseEntity<>(housingProviderService.getHousingProvidersForLoggedUser(), HttpStatus.OK);
    }

}
