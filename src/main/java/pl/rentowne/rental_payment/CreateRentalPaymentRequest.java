package pl.rentowne.rental_payment;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record CreateRentalPaymentRequest(
        @NotNull UUID requestId,
        @NotNull Mode mode,
        @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @Size(max = 500) String description
) {
    enum Mode { FULL, CUSTOM }

    @AssertTrue(message = "Podaj kwotę innej wpłaty; kwota pełnej wpłaty jest obliczana automatycznie.")
    public boolean isAmountValidForMode() {
        return mode == Mode.FULL ? amount == null : mode == Mode.CUSTOM && amount != null;
    }

    String payload() {
        return mode + "|" + (amount == null ? "" : amount.stripTrailingZeros().toPlainString())
                + "|" + paymentDate + "|" + normalizedDescription();
    }

    String normalizedDescription() {
        return description == null ? "" : description.strip();
    }
}
