package pl.rentowne.settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SettlementExportRowDto(
        String tenantFullName,
        String apartmentName,
        String rentedObjectName,
        String contractStatus,
        Long settlementId,
        LocalDateTime generatedDate,
        LocalDateTime date,
        BigDecimal compensationAmount,
        BigDecimal rentAndUtilitiesAmount,
        BigDecimal rentAmount,
        BigDecimal internetFee,
        BigDecimal gasDeposit,
        BigDecimal electricityAmount,
        BigDecimal distributedElectricityAmount,
        BigDecimal waterAmount,
        BigDecimal totalAmount,
        String prorationDescription,
        BigDecimal depositTopUp,
        BigDecimal additionalCharge,
        String additionalChargeDescription,
        List<SettlementChargeDto> additionalCharges
) {
}
