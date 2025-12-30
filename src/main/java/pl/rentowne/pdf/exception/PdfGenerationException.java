package pl.rentowne.pdf.exception;

import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Arrays;

/**
 * Wyjątek rzucany podczas generowania dokumentu PDF.
 */
@Getter
public class PdfGenerationException extends RuntimeException {

    private final transient PdfErrorCode errorCode;
    private final transient Object[] params;

    private PdfGenerationException(final PdfErrorCode errorCode, final Object... params) {
        super(errorCode.getMessage(params));
        this.errorCode = errorCode;
        this.params = params;
    }

    private PdfGenerationException(final PdfErrorCode errorCode, final Throwable cause, final Object... params) {
        super(errorCode.getMessage(params), cause);
        this.errorCode = errorCode;
        this.params = params;
    }

    /**
     * Tworzy wyjątek z kodem błędu.
     *
     * @param errorCode kod błędu
     * @param params    parametry do wiadomości
     * @return wyjątek
     */
    @NonNull
    public static PdfGenerationException error(final PdfErrorCode errorCode, final Object... params) {
        return new PdfGenerationException(errorCode, params);
    }

    /**
     * Tworzy wyjątek z kodem błędu i przyczyną.
     *
     * @param errorCode kod błędu
     * @param cause     przyczyna
     * @param params    parametry do wiadomości
     * @return wyjątek
     */
    @NonNull
    public static PdfGenerationException error(final PdfErrorCode errorCode, final Throwable cause, final Object... params) {
        return new PdfGenerationException(errorCode, cause, params);
    }

    /**
     * Tworzy wyjątek z kodem błędu i rozszerzonym opisem.
     *
     * @param errorCode kod błędu
     * @param params    parametry do opisu
     * @return wyjątek
     */
    @NonNull
    public static PdfGenerationException errorWithExtDescription(final PdfErrorCode errorCode, final Object... params) {
        return new PdfGenerationException(errorCode, params);
    }

    /**
     * Tworzy wyjątek z kodem błędu, przyczyną i rozszerzonym opisem.
     *
     * @param errorCode kod błędu
     * @param cause     przyczyna
     * @param params    parametry do opisu
     * @return wyjątek
     */
    @NonNull
    public static PdfGenerationException errorWithExtDescription(final PdfErrorCode errorCode, final Throwable cause, final Object... params) {
        return new PdfGenerationException(errorCode, cause, params);
    }

    /**
     * Zwraca rozszerzony opis błędu z parametrami.
     *
     * @return rozszerzony opis
     */
    @Nullable
    public String getExtendedDescription() {
        return errorCode.getDescription(params);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public String toString() {
        return "PdfGenerationException{" +
                "errorCode=" + errorCode +
                ", message=" + errorCode.getMessage(params) +
                ", description=" + errorCode.getDescription(params) +
                ", params=" + Arrays.toString(params) +
                '}';
    }
}
