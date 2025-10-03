package pl.rentowne.meter.model;

public enum MeterType {
    WATER_COLD,
    WATER_WARM,
    GAS,
    ELECTRIC,
    HEAT;

    public static MeterType fromString(String value) {
        for (MeterType type : MeterType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Nieznany typ licznika: " + value);
    }
}
