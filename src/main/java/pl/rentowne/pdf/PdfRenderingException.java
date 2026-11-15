package pl.rentowne.pdf;

public class PdfRenderingException extends RuntimeException {
    public PdfRenderingException(String message) { super(message); }
    public PdfRenderingException(String message, Throwable cause) { super(message, cause); }
}
