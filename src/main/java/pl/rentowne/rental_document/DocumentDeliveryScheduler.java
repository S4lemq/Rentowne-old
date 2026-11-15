package pl.rentowne.rental_document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "application.documents", name = "worker-enabled", havingValue = "true", matchIfMissing = true)
class DocumentDeliveryScheduler {
    private final DocumentDeliveryWorker worker;

    @Scheduled(fixedDelayString = "${application.documents.poll-delay-ms:5000}", scheduler = "documentScheduler")
    void poll() {
        try {
            for (int i = 0; i < 10 && worker.processNext(); i++) { /* bounded batch */ }
        } catch (RuntimeException error) {
            log.error("Document queue could not be processed ({})", error.getClass().getSimpleName());
        }
    }
}
