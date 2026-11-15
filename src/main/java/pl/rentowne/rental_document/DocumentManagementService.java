package pl.rentowne.rental_document;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Stable application facade; transactions and persistence belong to the focused services. */
@Service
@RequiredArgsConstructor
public class DocumentManagementService {
    private final DocumentQueryService queries;
    private final DocumentGenerationService generation;
    private final DocumentSendService sending;

    public DocumentDetail detail(UUID id) {
        return queries.detail(id);
    }

    public Map<Long, DocumentSummary> settlementSummaries(Collection<Long> ids) {
        return queries.settlementSummaries(ids);
    }

    public DocumentPage list(
            String tab,
            String type,
            String search,
            Long apartmentId,
            String mailState,
            LocalDate from,
            LocalDate to,
            Long settlementId,
            Long leaseId,
            Long tenantId,
            int page,
            int size) {
        return queries.list(
                tab,
                type,
                search,
                apartmentId,
                mailState,
                from,
                to,
                settlementId,
                leaseId,
                tenantId,
                page,
                size);
    }

    public List<DocumentApartment> apartments() {
        return queries.apartments();
    }

    public DocumentDetail regenerate(UUID id, long revision) {
        generation.regenerate(id, revision);
        return queries.detail(id);
    }

    public DocumentDetail deletePdf(UUID id, long revision) {
        generation.deletePdf(id, revision);
        return queries.detail(id);
    }

    public byte[] bytes(UUID id) {
        return generation.bytes(id);
    }

    public DocumentDetail send(UUID id, DocumentSendRequest request) {
        sending.send(id, request);
        return queries.detail(id);
    }
}
