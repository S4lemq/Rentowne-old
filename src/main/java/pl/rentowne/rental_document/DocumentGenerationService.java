package pl.rentowne.rental_document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import pl.rentowne.tenant.TenantRepository;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class DocumentGenerationService {
    private final RentalDocumentRepository repository;
    private final RentalDocumentService documents;
    private final DocumentMapper mapper;
    private final RentalDocumentPdfService pdf;
    private final DocumentFileStore files;
    private final DocumentAccess access;
    private final TenantRepository tenants;
    private final PlatformTransactionManager transactionManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void created(RentalDocumentService.Created event) {
        var tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            tx.executeWithoutResult(
                    status -> generate(repository.lockById(event.id()).orElseThrow(), false));
        } catch (RuntimeException error) {
            log.warn(
                    "PDF generation failed for {} ({})",
                    event.id(),
                    error.getClass().getSimpleName());
            try {
                tx.executeWithoutResult(
                        status ->
                                repository
                                        .lockById(event.id())
                                        .orElseThrow()
                                        .generationFailed(
                                                "Nie udało się przygotować PDF. Użyj przycisku"
                                                    + " Wygeneruj ponownie."));
            } catch (RuntimeException unavailable) {
                log.error("Could not persist PDF failure for {}", event.id());
            }
        }
    }

    @Transactional
    public void regenerate(UUID id, long revision) {
        var document = access.owned(id, true);
        access.checkRevision(document, revision);
        access.mutable(document);
        if (document.isCancelled()) throw DocumentAccess.conflict("Dokument jest anulowany.");
        generate(document, true);
    }

    @Transactional
    public void deletePdf(UUID id, long revision) {
        var document = access.owned(id, true);
        access.checkRevision(document, revision);
        access.mutable(document);
        String previous = document.getFilename();
        document.removePdf();
        deleteAfterCommit(previous);
    }

    @Transactional(readOnly = true)
    public byte[] bytes(UUID id) {
        var document = access.owned(id, false);
        try {
            return files.read(document.getFilename(), document.getChecksum());
        } catch (IOException error) {
            throw DocumentAccess.conflict(
                    "Nie można odczytać zapisanego PDF. Plik nie istnieje lub jest uszkodzony.");
        }
    }

    private void generate(RentalDocument document, boolean refresh) {
        if (document.isCancelled()) return;
        var data = refresh ? documents.refresh(document) : mapper.data(document);
        byte[] bytes = pdf.generate(data);
        String filename;
        try {
            filename = files.write(UUID.randomUUID(), bytes);
        } catch (IOException error) {
            throw new IllegalStateException("Nie można zapisać PDF", error);
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) files.deleteQuietly(filename);
                    }
                });
        String previous = document.getFilename();
        document.generated(data, mapper.encode(data), filename, DocumentFileStore.hash(bytes));
        if (refresh && document.getTenantId() != null) {
            tenants.findById(document.getTenantId())
                    .ifPresent(
                            tenant -> {
                                var owner = tenant.getRentedObject().getApartment().getOwner();
                                var cc =
                                        java.util.stream.Stream.of(
                                                        owner.getEmail(),
                                                        owner.getRepresentativeEmail())
                                                .filter(value -> value != null && !value.isBlank())
                                                .distinct()
                                                .toList();
                                document.recipients(tenant.getEmail(), mapper.encode(cc));
                            });
        }
        deleteAfterCommit(previous);
    }

    private void deleteAfterCommit(String filename) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        files.deleteQuietly(filename);
                    }
                });
    }
}
