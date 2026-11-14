package pl.rentowne.rental_payment;

import java.math.BigDecimal;
import java.time.LocalDate;

record RentalPaymentDto(Long id, BigDecimal amount, LocalDate paymentDate, String description, Long version) {
    static RentalPaymentDto from(RentalPayment payment) {
        return new RentalPaymentDto(payment.getId(), payment.getAmount(), payment.getPaymentDate(),
                payment.getDescription(), payment.getVersion());
    }
}
