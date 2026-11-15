package pl.rentowne.settlement;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
record ApartmentFinancialDetailDto(
        Long apartmentId,
        String apartmentName,
        BigDecimal administrativeFee,
        BigDecimal internet,
        BigDecimal electricity,
        BigDecimal gas,
        BigDecimal tax,
        BigDecimal revenue,
        BigDecimal costs,
        BigDecimal netIncome,
        BigDecimal water,
        BigDecimal other,
        BigDecimal operatingCosts,
        BigDecimal operatingResult,
        BigDecimal estimatedTax,
        BigDecimal resultAfterTax
) {
}
