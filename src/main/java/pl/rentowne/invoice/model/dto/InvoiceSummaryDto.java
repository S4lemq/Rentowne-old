package pl.rentowne.invoice.model.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record InvoiceSummaryDto (
    BigDecimal paidThisMonth,
    BigDecimal upcomingPayments,
    BigDecimal overduePayments,
    BigDecimal toSettleThisMonth
) {}
