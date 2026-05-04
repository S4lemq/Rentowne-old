package pl.rentowne.invoice;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
record InvoiceSummaryDto (
    BigDecimal paidThisMonth,
    BigDecimal upcomingPayments,
    BigDecimal overduePayments,
    BigDecimal toSettleThisMonth
) {}
