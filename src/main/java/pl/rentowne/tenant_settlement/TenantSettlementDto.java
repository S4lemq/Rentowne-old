package pl.rentowne.tenant_settlement;

import java.math.BigDecimal;

public record TenantSettlementDto(
        BigDecimal totalAmount,
        Long paymentId
) {
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Long getPaymentId() {
        return paymentId;
    }
}
