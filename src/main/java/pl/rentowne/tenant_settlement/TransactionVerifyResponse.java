package pl.rentowne.tenant_settlement;

import lombok.Getter;

@Getter
class TransactionVerifyResponse {
    private Data data;

    record Data (String status) {}
}
