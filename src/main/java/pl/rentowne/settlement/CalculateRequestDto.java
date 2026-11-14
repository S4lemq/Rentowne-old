package pl.rentowne.settlement;

import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;
import java.util.List;

@Builder
public record CalculateRequestDto(
        boolean waterIncluded,
        boolean electricityIncluded,
        @NotNull LocalDateTime settlementDate,
        Boolean isPartialMonth,
        LocalDateTime periodStartDate,
        LocalDateTime periodEndDate,
        Boolean isRentProrated,
        Boolean isCompensationProrated,
        Boolean isGasDepositProrated,
        Boolean isInternetFeeProrated,
        @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal additionalCharge,
        @Size(max = 500) String additionalChargeDescription,
        UUID requestId,
        Long tenantId,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Size(max = 100) List<@NotNull @Valid SettlementChargeDto> additionalCharges
) {
    @JsonIgnore
    @AssertTrue(message = "Okres rozliczenia musi mieć poprawną kolejność dat i mieścić się w miesiącu daty rozliczenia.")
    public boolean isPeriodValid() {
        if (!Boolean.TRUE.equals(isPartialMonth)) return true;
        if (periodStartDate == null || periodEndDate == null || settlementDate == null) return false;
        return !periodStartDate.toLocalDate().isAfter(periodEndDate.toLocalDate())
                && YearMonth.from(periodStartDate).equals(YearMonth.from(periodEndDate))
                && YearMonth.from(periodStartDate).equals(YearMonth.from(settlementDate));
    }
}
