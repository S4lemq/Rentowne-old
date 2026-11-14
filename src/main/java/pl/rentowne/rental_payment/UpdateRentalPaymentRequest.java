package pl.rentowne.rental_payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

record UpdateRentalPaymentRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @Size(max = 500) String description,
        @NotNull @PositiveOrZero Long version,
        @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal depositAmount
) {}
