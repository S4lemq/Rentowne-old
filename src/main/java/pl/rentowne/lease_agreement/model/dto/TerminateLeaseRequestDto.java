package pl.rentowne.lease_agreement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO dla żądania zakończenia umowy najmu
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminateLeaseRequestDto {

    private String chargeDescription; //opis obciążenia przy zakończeniu umowy
    private BigDecimal chargeAmount; //kwota obciążenia przy zakończeniu umowy
    private BigDecimal gasChargeAmount; //kwota obciążenia za gaz przy zakończeniu umowy
}

