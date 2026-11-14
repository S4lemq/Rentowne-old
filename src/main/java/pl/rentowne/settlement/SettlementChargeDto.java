package pl.rentowne.settlement;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SettlementChargeDto(
        @NotNull Type type,
        @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Size(max = 500) String description
) {
    public enum Type { DEPOSIT, OTHER }
}
