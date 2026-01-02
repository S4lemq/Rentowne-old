package pl.rentowne.pdf_template;

import lombok.experimental.UtilityClass;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;

import java.nio.charset.StandardCharsets;

/**
 * Pomocnik do tworzenia odpowiedzi HTTP dla dokumentów PDF wygenerowanych z szablonów.
 */
@UtilityClass
public class PdfTemplateResponseHelper {

    private static final MediaType APPLICATION_PDF = MediaType.APPLICATION_PDF;

    /**
     * Tworzy ResponseEntity dla dokumentu PDF z odpowiednimi nagłówkami (jako załącznik).
     *
     * @param response odpowiedź z dokumentem PDF
     * @return ResponseEntity gotowe do zwrócenia z kontrolera
     */
    @NonNull
    public ResponseEntity<ByteArrayResource> buildResponse(@NonNull PdfTemplateResponse response) {
        return buildResponse(response, false);
    }

    /**
     * Tworzy ResponseEntity dla dokumentu PDF z odpowiednimi nagłówkami.
     *
     * @param response odpowiedź z dokumentem PDF
     * @param inline   czy dokument ma być wyświetlany inline (w przeglądarce) czy jako załącznik
     * @return ResponseEntity gotowe do zwrócenia z kontrolera
     */
    @NonNull
    public ResponseEntity<ByteArrayResource> buildResponse(@NonNull PdfTemplateResponse response, boolean inline) {
        HttpHeaders headers = createHeaders(response, inline);

        // Cache headers - brak cachowania
        headers.setCacheControl(CacheControl.noStore().getHeaderValue());
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(response.content()));
    }

    /**
     * Tworzy ResponseEntity dla podglądu PDF w przeglądarce.
     *
     * @param response odpowiedź z dokumentem PDF
     * @return ResponseEntity z nagłówkami do wyświetlenia inline
     */
    @NonNull
    public ResponseEntity<ByteArrayResource> buildPreviewResponse(@NonNull PdfTemplateResponse response) {
        return buildResponse(response, true);
    }

    private HttpHeaders createHeaders(PdfTemplateResponse response, boolean inline) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_PDF);
        headers.setContentLength(response.size());

        ContentDisposition.Builder dispositionBuilder = inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment();

        headers.setContentDisposition(
                dispositionBuilder
                        .filename(response.filename(), StandardCharsets.UTF_8)
                        .build()
        );

        return headers;
    }
}

