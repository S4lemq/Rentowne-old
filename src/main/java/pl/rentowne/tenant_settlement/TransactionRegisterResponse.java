package pl.rentowne.tenant_settlement;

import lombok.Getter;

@Getter
class TransactionRegisterResponse {
    private Data data;

    record Data (String token){}
}
