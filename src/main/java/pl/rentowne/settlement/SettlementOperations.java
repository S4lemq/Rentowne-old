package pl.rentowne.settlement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import pl.rentowne.rental_payment.RentalPaymentException;

@Component
@RequiredArgsConstructor
class SettlementOperations {
    private final SettlementOperationRepository operations;
    private final ObjectMapper json;

    boolean alreadySaved(Long tenantId, Long rentedObjectId, CalculateRequestDto request) {
        if (request.requestId() == null) return false; // Compatibility with older clients.
        var previous = operations.findById(new SettlementOperation.Id(tenantId, request.requestId()));
        if (previous.isEmpty()) return false;
        SettlementOperation operation = previous.get();
        if (operation.getSettlement() == null || !payload(rentedObjectId, request).equals(operation.getPayload())) {
            throw new RentalPaymentException(HttpStatus.CONFLICT, "Ta operacja rozliczenia została już użyta lub cofnięta. Odśwież historię.");
        }
        return true;
    }

    void remember(Settlement settlement, CalculateRequestDto request) {
        if (request.requestId() != null) {
            operations.saveAndFlush(new SettlementOperation(settlement.getTenant().getId(), request.requestId(),
                    payload(settlement.getRentedObject().getId(), request), settlement));
        }
    }

    private String payload(Long rentedObjectId, CalculateRequestDto request) {
        try {
            return rentedObjectId + "|" + json.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Nieprawidłowe dane rozliczenia", e);
        }
    }
}
