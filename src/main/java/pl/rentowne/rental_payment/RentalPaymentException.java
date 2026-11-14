package pl.rentowne.rental_payment;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
class RentalPaymentException extends RuntimeException {
    private final HttpStatus status;

    RentalPaymentException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
