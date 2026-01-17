package pl.rentowne.settlement;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CalculateRequestDto {
    private boolean waterIncluded;
    private boolean electricityIncluded;
    private LocalDateTime settlementDate;

    // Partial month settlement fields
    private Boolean isPartialMonth;
    private LocalDateTime periodStartDate;
    private LocalDateTime periodEndDate;

    // Proration configuration for each component
    private Boolean isRentProrated;
    private Boolean isCompensationProrated;
    private Boolean isGasDepositProrated;
    private Boolean isInternetFeeProrated;
}
