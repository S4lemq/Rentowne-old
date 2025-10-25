package pl.rentowne.lease_agreement.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.rentowne.common.controler.AbstractController;
import pl.rentowne.lease_agreement.model.dto.GasCheckRequestDto;
import pl.rentowne.lease_agreement.model.dto.GasCheckResponseDto;
import pl.rentowne.lease_agreement.model.dto.LeaseTerminationInfoDto;
import pl.rentowne.lease_agreement.model.dto.TerminateLeaseRequestDto;
import pl.rentowne.lease_agreement.service.LeaseAgreementService;

@RestController
@RequiredArgsConstructor
@Tag(name = "Umowy")
public class LeaseAgreementController extends AbstractController {

    private final LeaseAgreementService leaseAgreementService;

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

}
