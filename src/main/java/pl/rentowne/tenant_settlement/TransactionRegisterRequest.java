package pl.rentowne.tenant_settlement;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
class TransactionRegisterRequest {
    private Integer merchantId;
    private Integer posId;
    private String sessionId;
    private Integer amount;
    private String currency;
    private String description;
    private String email;
    private String client;
    private String country;
    private String language;
    private String urlReturn;
    private String urlStatus;
    private String sign;
    private String encoding;
}
