package pl.rentowne.invoice_parser;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class PdfTextExtractor {

    public String extractText(MultipartFile file) {
        try {
            var resource = new InputStreamResource(file.getInputStream()) {
                @Override public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            var reader = new TikaDocumentReader(resource);

            List<Document> docs = reader.read();

            return docs.stream()
                    .map(Document::getText)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));

        } catch (IOException e) {
            throw new IllegalStateException("Nie udało się odczytać PDF: " + e.getMessage(), e);
        }
    }
}


