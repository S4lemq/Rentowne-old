package pl.rentowne.rental_document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class DocumentMapper {
    private final ObjectMapper json;

    RentalDocumentData data(RentalDocument document) {
        return decode(document.getSnapshot(), RentalDocumentData.class);
    }

    <T> T decode(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Niepoprawne dane dokumentu", error);
        }
    }

    List<String> addresses(String text) {
        try {
            return json.readValue(text, new TypeReference<>() {});
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Niepoprawny zapis odbiorców", error);
        }
    }

    String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Nie można zapisać danych dokumentu", error);
        }
    }

    DocumentHistory history(DocumentDelivery delivery) {
        return new DocumentHistory(
                delivery.getId(),
                delivery.getKind().name(),
                delivery.getState().name(),
                delivery.getTo(),
                addresses(delivery.getCcJson()),
                delivery.getCreatedAt(),
                delivery.getFinishedAt(),
                delivery.getError(),
                delivery.getRequestedBy());
    }

    static String filename(RentalDocumentData data, boolean cancelled) {
        return (cancelled ? "ANULOWANY-" : "")
                + data.type().filePrefix()
                + "-"
                + data.number().replace('/', '-')
                + ".pdf";
    }
}
