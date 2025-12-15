package pl.rentowne.settlement.model;

public enum ProrationMode {
    FULL("Pełny miesiąc"),
    PRORATED("Proporcjonalnie do dni");

    private final String description;

    ProrationMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

