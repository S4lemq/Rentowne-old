package pl.rentowne.rental_payment;

import java.math.BigDecimal;

public record RentalBalanceDto(
        BigDecimal totalCharges,
        BigDecimal historicalCredit,
        BigDecimal totalPayments,
        BigDecimal balance,
        BigDecimal amountDue,
        BigDecimal overpayment,
        PaymentStatus status
) {
    static RentalBalanceDto from(BigDecimal charges, BigDecimal historicalCredit, BigDecimal payments) {
        BigDecimal balance = payments.add(historicalCredit).subtract(charges);
        PaymentStatus status = balance.signum() > 0 ? PaymentStatus.OVERPAID
                : balance.signum() == 0 ? PaymentStatus.PAID
                : payments.signum() > 0 ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.DUE;
        return new RentalBalanceDto(charges, historicalCredit, payments, balance,
                balance.negate().max(BigDecimal.ZERO), balance.max(BigDecimal.ZERO), status);
    }
}
