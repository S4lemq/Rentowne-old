package pl.rentowne.tenant;
import pl.rentowne.settlement.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TenantSettlementRowDto(
        Long id,
        LocalDateTime settlementDate,
        BigDecimal totalAmount,
        SettlementStatus settlementStatus
) {
}
