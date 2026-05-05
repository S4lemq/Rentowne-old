package pl.rentowne.tenant_settlement;

public record NotificationReceiveDto(
        Integer merchantId,
        Integer posId,
        String sessionId,
        Integer amount,
        Integer originAmount,
        String currency,
        Long orderId,
        Integer methodId,
        String statement,
        String sign
) {
    public Integer getMerchantId() {
        return merchantId;
    }

    public Integer getPosId() {
        return posId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Integer getAmount() {
        return amount;
    }

    public Integer getOriginAmount() {
        return originAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Integer getMethodId() {
        return methodId;
    }

    public String getStatement() {
        return statement;
    }

    public String getSign() {
        return sign;
    }
}
