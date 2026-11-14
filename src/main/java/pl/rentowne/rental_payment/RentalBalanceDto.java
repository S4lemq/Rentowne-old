package pl.rentowne.rental_payment;

import java.math.BigDecimal;

public record RentalBalanceDto(
        BigDecimal totalCharges,
        BigDecimal historicalCredit,
        BigDecimal totalPayments,
        BigDecimal balance,
        BigDecimal amountDue,
        BigDecimal overpayment,
        PaymentStatus status,
        BigDecimal depositDue,
        BigDecimal depositReminder,
        BigDecimal depositToCharge,
        boolean depositAssessed,
        BigDecimal reservedDeposit,
        BigDecimal depositRequired,
        BigDecimal depositPaid
) {
    static RentalBalanceDto from(BigDecimal charges, BigDecimal historicalCredit, BigDecimal payments) {
        return from(charges, historicalCredit, payments, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO, null, null);
    }

    static RentalBalanceDto from(RentalPaymentRepository.BalanceTotals totals) {
        return excluding(totals, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    static RentalBalanceDto excluding(RentalPaymentRepository.BalanceTotals t, BigDecimal amount, BigDecimal deposit) {
        return from(t.getTotalCharges(), t.getHistoricalCredit(), t.getTotalPayments().subtract(amount),
                t.getDepositCharges(), t.getDepositCarryIn(), t.getDepositPayments().subtract(deposit),
                t.getDepositAssessed(), t.getDepositReminder(), t.getDepositRequired(),
                t.getDepositPaid() == null ? null : t.getDepositPaid().subtract(deposit));
    }

    static RentalBalanceDto from(BigDecimal charges, BigDecimal historicalCredit, BigDecimal payments,
                                 BigDecimal depositCharges, BigDecimal carryIn, BigDecimal depositPayments,
                                 boolean assessed, BigDecimal reminder, BigDecimal requiredDeposit, BigDecimal receivedDeposit) {
        // Receipts already held as deposit before a replacement charge are not free credit.
        // Keep carryIn fixed while the charge exists, including after receipt corrections.
        BigDecimal reserved = assessed ? carryIn : depositPayments;
        BigDecimal balance = payments.add(historicalCredit).subtract(charges).subtract(reserved);
        BigDecimal depositDue = assessed ? depositCharges.add(carryIn).subtract(depositPayments).max(BigDecimal.ZERO) : BigDecimal.ZERO;
        PaymentStatus status = balance.signum() > 0 ? PaymentStatus.OVERPAID
                : balance.signum() == 0 ? PaymentStatus.PAID
                : payments.signum() > 0 ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.DUE;
        return new RentalBalanceDto(charges, historicalCredit, payments, balance,
                balance.negate().max(BigDecimal.ZERO), balance.max(BigDecimal.ZERO), status,
                depositDue, reminder, reminder == null ? BigDecimal.ZERO : reminder.subtract(depositDue).max(BigDecimal.ZERO),
                assessed, reserved, requiredDeposit, receivedDeposit);
    }
}
