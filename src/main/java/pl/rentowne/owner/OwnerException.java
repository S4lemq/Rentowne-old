package pl.rentowne.owner;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OwnerException extends RuntimeException {
    private final HttpStatus status;

    public OwnerException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
