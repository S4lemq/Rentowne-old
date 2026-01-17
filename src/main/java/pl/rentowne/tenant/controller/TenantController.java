package pl.rentowne.tenant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.common.controler.AbstractController;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.exception.RentowneNotFoundException;
import pl.rentowne.tenant.model.dto.TenantDto;
import pl.rentowne.tenant.model.dto.TenantRowDto;
import pl.rentowne.tenant.model.dto.TenantSettlementRowDto;
import pl.rentowne.tenant.repository.TenantRepository;
import pl.rentowne.tenant.service.TenantService;
import pl.rentowne.user.service.UserService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Najemcy")
public class TenantController extends AbstractController {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final UserService userService;

    @Operation(
            summary = "Zapisuje najemcę wraz z umową najmu",
            description = "Metoda służy do zapisu najemcy i umowy najmu",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Najemcę oraz umowę dodano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PostMapping("/api/tenants")
    public ResponseEntity<TenantDto> addTenantAndLeaseAgreement(@RequestBody TenantDto tenantDto) throws RentowneBusinessException {
        Long tenantId = tenantService.addTenantAndLeaseAgreement(tenantDto);
        return new ResponseEntity<>(tenantService.getTenantAndLeaseAgreement(tenantId), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Pobiera najemcę oraz umowę po id",
            description = "Metoda służy do pobrania najemcy oraz umowę po id",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Najemcę oraz umowę pobrano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/tenants/{id}")
    public ResponseEntity<TenantDto> getTenantAndLeaseAgreement(@Parameter(description = "id najemcy", required = true) @PathVariable Long id) throws RentowneNotFoundException {
        return new ResponseEntity<>(tenantService.getTenantAndLeaseAgreement(id), HttpStatus.OK);
    }

    @Operation(
            summary = "Aktualizuje dane najemcy i/oraz umowy",
            description = "Metoda służy do aktualizowania danych najemcy i/oraz umowy",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dane najemcy i umowy zaktualizowano pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono danych"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PutMapping("/api/tenants")
    public ResponseEntity<Void> updateTenant(@Parameter(description = "dto najemcy", required = true) @RequestBody TenantDto dto) {
        tenantService.updateTenantAndLeaseAgreement(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Pobiera listę najemców z paginacją",
            description = "Metoda zwracająca stronicowaną listę najemców zalogowanego użytkownika z możliwością wyszukiwania",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sukces"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/tenants/list")
    public ResponseEntity<Page<TenantRowDto>> getTenantsList(
            Pageable pageable,
            @RequestParam(required = false) String searchText
    ) throws RentowneBusinessException {
        Long userId = userService.getLoggedUser().getId();
        Page<TenantRowDto> tenants = tenantRepository.getTenantsList(pageable, userId, searchText);
        return ResponseEntity.ok(tenants);
    }

    @Operation(
            summary = "Pobiera listę rozliczeń najemcy z paginacją",
            description = "Metoda zwracająca stronicowaną listę rozliczeń dla konkretnego obiektu do wynajęcia",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sukces"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/tenants/settlements-list")
    public ResponseEntity<Page<TenantSettlementRowDto>> getTenantSettlementsList(
            Pageable pageable,
            @RequestParam Long rentedObjectId
    ) {
        Page<TenantSettlementRowDto> settlements = tenantRepository.getTenantSettlementsList(pageable, rentedObjectId);
        return ResponseEntity.ok(settlements);
    }
}
