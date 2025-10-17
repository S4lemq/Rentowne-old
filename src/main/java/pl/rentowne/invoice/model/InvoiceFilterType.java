package pl.rentowne.invoice.model;

import lombok.Getter;

/**
 * Typ filtra dla faktur
 */
@Getter
public enum InvoiceFilterType {
    /**
     * Faktury zapłacone w bieżącym miesiącu (według paymentDate)
     */
    PAID_THIS_MONTH("PAID_THIS_MONTH"),

    /**
     * Zbliżające się płatności (aktualna data <= payDay i brak paymentDate)
     */
    UPCOMING_PAYMENTS("UPCOMING_PAYMENTS"),

    /**
     * Przeterminowane płatności (aktualna data > payDay i brak paymentDate)
     */
    OVERDUE_PAYMENTS("OVERDUE_PAYMENTS"),

    /**
     * Do rozliczenia w bieżącym miesiącu (według payDayMonth)
     */
    TO_SETTLE_THIS_MONTH("TO_SETTLE_THIS_MONTH");

    private final String value;

    InvoiceFilterType(String value) {
        this.value = value;
    }

    /**
     * Konwertuje String na enum InvoiceFilterType
     *
     * @param value wartość String do konwersji
     * @return odpowiednia wartość enum lub null jeśli nie znaleziono
     */
    public static InvoiceFilterType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (InvoiceFilterType type : InvoiceFilterType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }

        return null;
    }
}
