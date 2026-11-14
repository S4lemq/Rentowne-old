package pl.rentowne.rental_payment;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class RentalBalanceTest {
    @Test
    void shouldCarryDebtAndOverpaymentAcrossMonths() {
        assertBalance("2539.89", "0", "-2539.89", PaymentStatus.DUE);
        assertBalance("2539.89", "1000", "-1539.89", PaymentStatus.PARTIALLY_PAID);
        assertBalance("2539.89", "1800", "-739.89", PaymentStatus.PARTIALLY_PAID);
        assertBalance("5139.89", "1800", "-3339.89", PaymentStatus.PARTIALLY_PAID);
        assertBalance("5139.89", "5200", "60.11", PaymentStatus.OVERPAID);
        assertBalance("7639.89", "5200", "-2439.89", PaymentStatus.PARTIALLY_PAID);
        assertBalance("7639.89", "7639.89", "0", PaymentStatus.PAID);
        assertBalance("0", "0", "0", PaymentStatus.PAID);
    }

    @Test
    void shouldKeepHistoricalCoverageOutOfRealReceipts() {
        var result = RentalBalanceDto.from(new BigDecimal("7000"), new BigDecimal("5000"), new BigDecimal("2150"));
        assertThat(result.balance()).isEqualByComparingTo("150");
        assertThat(result.totalPayments()).isEqualByComparingTo("2150");
        assertThat(result.overpayment()).isEqualByComparingTo("150");
        assertThat(result.amountDue()).isZero();
    }

    private void assertBalance(String charges, String payments, String balance, PaymentStatus status) {
        var result = RentalBalanceDto.from(new BigDecimal(charges), BigDecimal.ZERO, new BigDecimal(payments));
        assertThat(result.balance()).isEqualByComparingTo(balance);
        assertThat(result.status()).isEqualTo(status);
    }
}
