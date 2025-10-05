package pl.rentowne.invoice;

import pl.rentowne.provider_field.model.BillingMethod;

import java.math.BigDecimal;

public record InvoiceLine(
        String name,
        BigDecimal price,
        BillingMethod billingMethod
) {}