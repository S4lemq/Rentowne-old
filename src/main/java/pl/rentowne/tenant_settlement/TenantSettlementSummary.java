package pl.rentowne.tenant_settlement;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TenantSettlementSummary(
        Long id,
        LocalDateTime placeDate,
        TenantSettlementStatus status,
        BigDecimal grossValue,
        Payment payment,
        String redirectUrl
) {
}
