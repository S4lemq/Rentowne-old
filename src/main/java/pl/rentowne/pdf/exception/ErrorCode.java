package pl.rentowne.pdf.exception;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

/**
 * Interfejs dla kodów błędów.
 */
public interface ErrorCode {

    HttpStatus getHttpStatus();

    String getMessage();

    String getMessage(@Nullable Object... params);

    String getDescription();

    String getDescription(@Nullable Object... params);
}

