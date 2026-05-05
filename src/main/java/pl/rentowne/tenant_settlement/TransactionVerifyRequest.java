package pl.rentowne.tenant_settlement;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
class TransactionVerifyRequest {
    private Integer merchantId;
    private Integer posId;
    private String sessionId;
    private Integer amount;
    private String currency;
    private Long orderId;
    private String sign;
}
