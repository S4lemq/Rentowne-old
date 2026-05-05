package pl.rentowne.settlement;

public enum SettlementStatus {
    TO_PAY("Do zapłaty"),
    PROCESSING("Przetwarzanie"),
    PAID("Opłacone");

    private String value;

    SettlementStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

