package pl.rentowne.invoice;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
record InvoiceFilterSummaryDto(
    BigDecimal totalAmount,
    BigDecimal totalConsumption
) {}

