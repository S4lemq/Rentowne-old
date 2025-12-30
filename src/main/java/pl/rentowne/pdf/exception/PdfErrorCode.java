package pl.rentowne.pdf.exception;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

/**
 * Kody błędów generowania dokumentów PDF.
 */
@Getter
public enum PdfErrorCode implements ErrorCode {

    FONT_LOADING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Nie można załadować czcionki: %s"),
    CONTENT_STREAM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Strumień treści nie jest otwarty - dodaj stronę najpierw"),
    PAGE_CREATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Nie można utworzyć strumienia treści strony"),
    TEXT_WIDTH_CALCULATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd podczas obliczania szerokości tekstu"),
    PARAGRAPH_CREATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd podczas tworzenia paragrafu"),
    TABLE_ROW_CREATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd podczas tworzenia wiersza tabeli"),
    LOGO_ADDITION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd podczas dodawania logo"),
    HORIZONTAL_LINE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd podczas rysowania linii"),
    DOCUMENT_SAVE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd podczas zapisywania dokumentu PDF"),
    DOCUMENT_CLOSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd podczas zamykania dokumentu PDF"),
    CONTENT_STREAM_CLOSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd podczas zamykania strumienia treści"),
    PAGE_NUMBERING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd podczas dodawania numeracji stron"),
    INVALID_IMAGE_DIMENSIONS(HttpStatus.BAD_REQUEST, "Wymiary obrazu muszą być większe od 0", "Podane wymiary: width=%s, height=%s"),
    EMPTY_IMAGE_BYTES(HttpStatus.BAD_REQUEST, "Bajty obrazu nie mogą być puste"),
    INVALID_SPACE_VALUE(HttpStatus.BAD_REQUEST, "Odstęp musi być większy od 0", "Podana wartość: %s"),
    DOCUMENT_GENERATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd podczas generowania dokumentu PDF");

    private final HttpStatus httpStatus;
    private final String message;
    private final String description;

    PdfErrorCode(final HttpStatus httpStatus, final String message, final String description) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.description = description;
    }

    PdfErrorCode(final HttpStatus httpStatus, final String message) {
        this(httpStatus, message, "");
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getMessage(@Nullable final Object... params) {
        return ArrayUtils.isEmpty(params)
                ? message
                : String.format(message, params);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getDescription(@Nullable final Object... params) {
        return ArrayUtils.isEmpty(params)
                ? description
                : String.format(description, params);
    }
}
