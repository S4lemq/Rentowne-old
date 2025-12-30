package pl.rentowne.common.pdf;

import org.springframework.lang.Nullable;

import java.io.Serial;

/**
 * Wyjątek rzucany podczas błędów generowania dokumentów PDF.
 */
final class PdfGenerationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    PdfGenerationException(@Nullable final String message) {
        super(message);
    }

    PdfGenerationException(@Nullable final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}

