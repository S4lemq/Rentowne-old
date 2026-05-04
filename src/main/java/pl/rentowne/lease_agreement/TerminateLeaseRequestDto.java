package pl.rentowne.lease_agreement;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * DTO dla żądania zakończenia umowy najmu
 */
@Builder
record TerminateLeaseRequestDto(
        String chargeDescription, //opis obciążenia przy zakończeniu umowy
        BigDecimal chargeAmount, //kwota obciążenia przy zakończeniu umowy
        BigDecimal gasChargeAmount //kwota obciążenia za gaz przy zakończeniu umowy
) {}
