package pl.rentowne.lease_agreement.service;

import pl.rentowne.lease_agreement.model.dto.GasCheckRequestDto;
import pl.rentowne.lease_agreement.model.dto.GasCheckResponseDto;
import pl.rentowne.lease_agreement.model.dto.LeaseTerminationInfoDto;
import pl.rentowne.lease_agreement.model.dto.TerminateLeaseRequestDto;

/**
 * Serwis umów najmu
 */
public interface LeaseAgreementService {

    /**
     * Pobiera informacje potrzebne do zakończenia umowy (koszty mediów i wpłacona kaucja)
     * @param leaseAgreementId id umowy najmu
     * @return informacje o kosztach i zużyciu mediów oraz wpłaconej kaucji
     */
    LeaseTerminationInfoDto getLeaseTerminationInfo(Long leaseAgreementId);

    /**
     * Zakończa umowę najmu - zapisuje dane obciążenia, rozlicza media i zwraca kaucję
     * @param leaseAgreementId id umowy najmu
     * @param request dane do zakończenia umowy (opis i kwoty obciążeń)
     */
    void terminateLease(Long leaseAgreementId, TerminateLeaseRequestDto request);

    /**
     * Sprawdza rozliczenie gazu dla danej umowy w określonym zakresie dat
     * @param leaseAgreementId id umowy najmu
     * @param request zakres dat od-do
     * @return informacje o sumie faktur, sumie zaliczek i saldzie
     */
    GasCheckResponseDto checkGasBalance(Long leaseAgreementId, GasCheckRequestDto request);
}
