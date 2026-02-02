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
import pl.rentowne.tenant.model.dto.DecryptedPasswordResponse;
import pl.rentowne.tenant.model.dto.PasswordInfoResponse;
import pl.rentowne.tenant.model.dto.SetPasswordRequest;
import pl.rentowne.tenant.model.dto.TenantDto;
import pl.rentowne.tenant.model.dto.TenantRowDto;
import pl.rentowne.tenant.model.dto.TenantSettlementRowDto;
import pl.rentowne.tenant.repository.TenantRepository;
import pl.rentowne.tenant.service.TenantPasswordService;
import pl.rentowne.tenant.service.TenantService;
import pl.rentowne.user.service.UserService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Najemcy")
public class TenantController extends AbstractController {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final UserService userService;
    private final TenantPasswordService tenantPasswordService;

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

    @Operation(
            summary = "Ustawia własne hasło PDF dla najemcy",
            description = "Metoda służy do ustawienia własnego hasła do dokumentów PDF. " +
                    "Hasła muszą być zakodowane w Base64. " +
                    "Hasło musi mieć od 12 do 512 znaków (po dekodowaniu). " +
                    "Oba hasła muszą być identyczne.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Hasło ustawiono pomyślnie"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe (hasła nie są identyczne lub nie spełniają wymagań długości)"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono najemcy"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @PutMapping("/api/tenants/{id}/pdf-password")
    public ResponseEntity<Void> setTenantPdfPassword(
            @Parameter(description = "ID najemcy", required = true) @PathVariable final Long id,
            @RequestBody final SetPasswordRequest request
    ) {
        tenantPasswordService.setCustomPassword(id, request.password(), request.confirmPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Usuwa własne hasło PDF najemcy",
            description = "Metoda służy do usunięcia własnego hasła PDF. " +
                    "Po usunięciu najemca będzie używał hasła domyślnego (2x ostatnie 6 cyfr CIF).",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Hasło usunięto pomyślnie"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono najemcy"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @DeleteMapping("/api/tenants/{id}/pdf-password")
    public ResponseEntity<Void> removeTenantPdfPassword(
            @Parameter(description = "ID najemcy", required = true) @PathVariable final Long id
    ) {
        tenantPasswordService.removeCustomPassword(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Pobiera informacje o haśle PDF najemcy",
            description = "Metoda zwraca informację czy najemca ma ustawione własne hasło czy używa domyślnego.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sukces"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono najemcy"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/tenants/{id}/pdf-password/info")
    public ResponseEntity<PasswordInfoResponse> getTenantPasswordInfo(
            @Parameter(description = "ID najemcy", required = true) @PathVariable final Long id
    ) {
        final boolean hasCustomPassword = tenantPasswordService.hasCustomPassword(id);
        final PasswordInfoResponse response = hasCustomPassword
                ? PasswordInfoResponse.custom()
                : PasswordInfoResponse.defaultPassword();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Pobiera hasło PDF najemcy (tylko dla administratora)",
            description = "Metoda służy do odzyskania hasła PDF dla klienta który je zapomniał. " +
                    "Wymaga uprawnień administratora. Zwraca hasło zakodowane w Base64.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Hasło pobrano pomyślnie"),
                    @ApiResponse(responseCode = "403", description = "Brak uprawnień"),
                    @ApiResponse(responseCode = "404", description = "Nie znaleziono najemcy"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/admin/tenants/{id}/pdf-password")
    public ResponseEntity<DecryptedPasswordResponse> getDecryptedPassword(
            @Parameter(description = "ID najemcy", required = true) @PathVariable final Long id
    ) {
        final boolean hasCustomPassword = tenantPasswordService.hasCustomPassword(id);
        final String passwordBase64 = tenantPasswordService.getPdfPasswordBase64Encoded(id);
        return ResponseEntity.ok(new DecryptedPasswordResponse(passwordBase64, hasCustomPassword));
    }
}
