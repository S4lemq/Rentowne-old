package pl.rentowne.invoice;

import pl.rentowne.provider_field.model.BillingMethod;

import java.math.BigDecimal;

record FieldDiff(
        String name,
        BigDecimal dbPrice,
        BigDecimal pdfPrice,
        BillingMethod dbBilling,
        BillingMethod pdfBilling,
        String reason // ADDED_IN_DB, MISSING_IN_DB, PRICE_CHANGED, BILLING_CHANGED, NAME_ONLY_IN_PDF, NAME_ONLY_IN_DB
) {}