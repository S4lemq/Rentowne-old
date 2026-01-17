package pl.rentowne.lease_agreement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.common.controler.AbstractController;
import pl.rentowne.exception.RentowneBusinessException;
import pl.rentowne.lease_agreement.model.dto.GasCheckRequestDto;
import pl.rentowne.lease_agreement.model.dto.GasCheckResponseDto;
import pl.rentowne.lease_agreement.model.dto.LeaseAgreementRowDto;
import pl.rentowne.lease_agreement.model.dto.LeaseTerminationInfoDto;
import pl.rentowne.lease_agreement.model.dto.TerminateLeaseRequestDto;
import pl.rentowne.lease_agreement.repository.LeaseAgreementRepository;
import pl.rentowne.lease_agreement.service.LeaseAgreementService;
import pl.rentowne.user.service.UserService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Umowy")
public class LeaseAgreementController extends AbstractController {

    private final LeaseAgreementService leaseAgreementService;
    private final LeaseAgreementRepository leaseAgreementRepository;
    private final UserService userService;

    /**
     * Pobiera informacje potrzebne do zakończenia umowy (koszty mediów i wpłacona kaucja)
     */
    @GetMapping("/api/lease-agreements/{leaseAgreementId}/termination-info")
    public ResponseEntity<LeaseTerminationInfoDto> getLeaseTerminationInfo(
            @PathVariable Long leaseAgreementId) {
        LeaseTerminationInfoDto info = leaseAgreementService.getLeaseTerminationInfo(leaseAgreementId);
        return ResponseEntity.ok(info);
    }

    /**
     * Zakończa umowę najmu - zapisuje dane obciążeń, rozlicza media i zwraca kaucję
     */
    @PostMapping("/api/lease-agreements/{leaseAgreementId}/terminate")
    public ResponseEntity<Void> terminateLease(
            @PathVariable Long leaseAgreementId,
            @RequestBody TerminateLeaseRequestDto request) {
        leaseAgreementService.terminateLease(leaseAgreementId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Sprawdza rozliczenie gazu dla umowy w określonym zakresie dat
     */
    @PostMapping("/api/lease-agreements/{leaseAgreementId}/gas-check")
    public ResponseEntity<GasCheckResponseDto> checkGasBalance(
            @PathVariable Long leaseAgreementId,
            @RequestBody GasCheckRequestDto request) {
        GasCheckResponseDto response = leaseAgreementService.checkGasBalance(leaseAgreementId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Oblicza podgląd zwrotu kaucji bez zapisywania danych
     */
    @PostMapping("/api/lease-agreements/{leaseAgreementId}/deposit-return-preview")
    public ResponseEntity<String> previewDepositReturn(
            @PathVariable Long leaseAgreementId,
            @RequestBody TerminateLeaseRequestDto request) {
        String message = leaseAgreementService.previewDepositReturn(leaseAgreementId, request);
        return ResponseEntity.ok(message);
    }

    @Operation(
            summary = "Pobiera listę umów najmu z paginacją",
            description = "Metoda zwracająca stronicowaną listę umów najmu zalogowanego użytkownika z możliwością filtrowania",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sukces"),
                    @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe"),
                    @ApiResponse(responseCode = "500", description = "Wewnętrzny błąd serwera")
            }
    )
    @GetMapping("/api/lease-agreements/list")
    public ResponseEntity<Page<LeaseAgreementRowDto>> getLeaseAgreementsList(
            Pageable pageable,
            @RequestParam(required = false) Boolean isContractActive,
            @RequestParam(required = false) String searchText
    ) throws RentowneBusinessException {
        Long userId = userService.getLoggedUser().getId();
        Page<LeaseAgreementRowDto> leaseAgreements = leaseAgreementRepository.getLeaseAgreementsList(
                pageable, userId, isContractActive, searchText
        );
        return ResponseEntity.ok(leaseAgreements);
    }

}
