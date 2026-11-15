package pl.rentowne.lease_agreement;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

/**
 * DTO dla żądania zakończenia umowy najmu
 */
@Builder
record TerminateLeaseRequestDto(
        @Size(max = 500) String chargeDescription,
        @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal chargeAmount,
        @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal gasChargeAmount,
        LocalDate terminationDate,
        LocalDate refundDueDate
) {}
