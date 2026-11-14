package pl.rentowne.rental_payment;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RentalPaymentException extends RuntimeException {
    private final HttpStatus status;

    public RentalPaymentException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
