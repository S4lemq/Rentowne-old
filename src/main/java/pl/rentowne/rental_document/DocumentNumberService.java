package pl.rentowne.rental_document;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
class DocumentNumberService {
    private final DocumentOwnerRepository owners;
    private final RentalDocumentCounterRepository counters;

    @Transactional(propagation = Propagation.MANDATORY)
    int next(Long ownerId, RentalDocumentType type, int year) {
        owners.lockById(ownerId).orElseThrow();
        var key = new RentalDocumentCounter.Key(ownerId, type, year);
        var counter = counters.findById(key).orElseGet(() -> new RentalDocumentCounter(key));
        int value = counter.next();
        counters.saveAndFlush(counter);
        return value;
    }
}
