package pl.rentowne.pdf;

import lombok.experimental.UtilityClass;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Pomocnik do tworzenia odpowiedzi HTTP dla dokumentów PDF.
 */
@UtilityClass
final class PdfResponseHelper {

    private static final MediaType APPLICATION_PDF = MediaType.APPLICATION_PDF;

    /**
     * Tworzy ResponseEntity dla dokumentu PDF z odpowiednimi nagłówkami.
     *
     * @param response odpowiedź z dokumentem PDF
     * @return ResponseEntity gotowe do zwrócenia z kontrolera
     */
    @NonNull
    ResponseEntity<ByteArrayResource> buildResponse(@NonNull final PdfDocumentResponse response) {
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
    ResponseEntity<ByteArrayResource> buildResponse(@NonNull final PdfDocumentResponse response, final boolean inline) {
        final HttpHeaders headers = createHeaders(response, inline);

        // Cache headers - prywatne, krótkie cachowanie
        headers.setCacheControl(CacheControl.noStore().getHeaderValue());
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(response.content()));
    }

    /**
     * Tworzy ResponseEntity dla dokumentu PDF z cachowaniem.
     *
     * @param response      odpowiedź z dokumentem PDF
     * @param cacheDuration czas cachowania
     * @return ResponseEntity gotowe do zwrócenia z kontrolera
     */
    @NonNull
    ResponseEntity<ByteArrayResource> buildCacheableResponse(
            @NonNull final PdfDocumentResponse response,
            @NonNull final Duration cacheDuration
    ) {
        final HttpHeaders headers = createHeaders(response, false);

        // Cache headers - prywatne, z określonym czasem
        headers.setCacheControl(CacheControl.maxAge(cacheDuration).cachePrivate().getHeaderValue());

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(response.content()));
    }

    @NonNull
    private HttpHeaders createHeaders(@NonNull final PdfDocumentResponse response, final boolean inline) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_PDF);
        headers.setContentLength(response.size());

        final ContentDisposition contentDisposition = inline
                ? ContentDisposition.inline()
                        .filename(response.filename(), StandardCharsets.UTF_8)
                        .build()
                : ContentDisposition.attachment()
                        .filename(response.filename(), StandardCharsets.UTF_8)
                        .build();

        headers.setContentDisposition(contentDisposition);
        return headers;
    }
}
