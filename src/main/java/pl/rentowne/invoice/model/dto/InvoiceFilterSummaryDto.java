package pl.rentowne.invoice.model.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record InvoiceFilterSummaryDto(
    BigDecimal totalAmount,
    BigDecimal totalConsumption
) {}

