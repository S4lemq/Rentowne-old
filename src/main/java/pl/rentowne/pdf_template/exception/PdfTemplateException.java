package pl.rentowne.pdf_template.exception;

/**
 * Wyjątek rzucany podczas błędów generowania PDF z szablonów.
 */
public class PdfTemplateException extends RuntimeException {

    private final PdfTemplateErrorCode errorCode;

    public PdfTemplateException(PdfTemplateErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public PdfTemplateException(PdfTemplateErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public PdfTemplateException(PdfTemplateErrorCode errorCode, String details) {
        super(errorCode.getMessage() + ": " + details);
        this.errorCode = errorCode;
    }

    public PdfTemplateException(PdfTemplateErrorCode errorCode, String details, Throwable cause) {
        super(errorCode.getMessage() + ": " + details, cause);
        this.errorCode = errorCode;
    }

    public PdfTemplateErrorCode getErrorCode() {
        return errorCode;
    }
}

