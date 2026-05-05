package pl.rentowne.settlement;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CalculateRequestDto(
        boolean waterIncluded,
        boolean electricityIncluded,
        LocalDateTime settlementDate,
        Boolean isPartialMonth,
        LocalDateTime periodStartDate,
        LocalDateTime periodEndDate,
        Boolean isRentProrated,
        Boolean isCompensationProrated,
        Boolean isGasDepositProrated,
        Boolean isInternetFeeProrated
) {
}
