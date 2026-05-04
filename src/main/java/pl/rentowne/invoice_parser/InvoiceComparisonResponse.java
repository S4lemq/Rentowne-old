package pl.rentowne.invoice_parser;

import pl.rentowne.provider_field.BillingMethod;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceComparisonResponse(
        BasicInfoChanges basicInfoChanges,
        List<FieldChange> modifiedFields,
        List<AddedField> addedFields,
        List<RemovedField> removedFields,
        boolean hasChanges
) {
    public record BasicInfoChanges(
            ValueChange<BigDecimal> tax,
            ValueChange<BigDecimal> conversionRate
    ) {
        public boolean hasAnyChange() {
            return (tax != null && tax.hasChange()) ||
                   (conversionRate != null && conversionRate.hasChange());
        }
    }

    public record ValueChange<T>(
            T oldValue,
            T newValue
    ) {
        public boolean hasChange() {
            if (oldValue == null && newValue == null) return false;
            if (oldValue == null || newValue == null) return true;
            return !oldValue.equals(newValue);
        }
    }

    public record FieldChange(
            String fieldName,
            BigDecimal oldPrice,
            BigDecimal newPrice,
            BillingMethod oldBillingMethod,
            BillingMethod newBillingMethod
    ) {
        public boolean hasPriceChange() {
            return oldPrice != null && newPrice != null && oldPrice.compareTo(newPrice) != 0;
        }

        public boolean hasBillingMethodChange() {
            return oldBillingMethod != null && newBillingMethod != null && !oldBillingMethod.equals(newBillingMethod);
        }
    }

    public record AddedField(
            String fieldName,
            BigDecimal price,
            BillingMethod billingMethod
    ) {}

    public record RemovedField(
            String fieldName,
            BigDecimal price,
            BillingMethod billingMethod
    ) {}
}

