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

/**
 * Pomocnik do tworzenia odpowiedzi HTTP dla dokumentów PDF.
 */
@UtilityClass
final class PdfResponseHelper {

    private static final MediaType APPLICATION_PDF = MediaType.APPLICATION_PDF;

    /**
     * Tworzy ResponseEntity dla dokumentu PDF z odpowiednimi nagłówkami.
     * <p>
     * Używa {@link PdfDocumentResponse#contentUnsafe()} dla wydajności,
     * ponieważ {@link ByteArrayResource} nie modyfikuje przekazanej tablicy.
     * </p>
     *
     * @param response odpowiedź z dokumentem PDF
     * @return ResponseEntity gotowe do zwrócenia z kontrolera
     */
    @NonNull
    ResponseEntity<ByteArrayResource> buildResponse(@NonNull final PdfDocumentResponse response) {
        final HttpHeaders headers = createHeaders(response);

        // Cache headers - prywatne, krótkie cachowanie
        headers.setCacheControl(CacheControl.noStore().getHeaderValue());
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(response.contentUnsafe()));
    }


    @NonNull
    private HttpHeaders createHeaders(@NonNull final PdfDocumentResponse response) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_PDF);
        headers.setContentLength(response.size());

        final ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(response.filename(), StandardCharsets.UTF_8)
                .build();

        headers.setContentDisposition(contentDisposition);
        return headers;
    }
}
