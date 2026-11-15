package pl.rentowne.rental_document;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.rentowne.user.UserService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class DocumentQueryService {
    private final RentalDocumentRepository documents;
    private final DocumentDeliveryRepository deliveries;
    private final DocumentAccess access;
    private final DocumentMapper mapper;
    private final UserService users;

    DocumentDetail detail(UUID id) {
        var d = access.owned(id, false);
        var history = deliveries.findByDocumentIdOrderByCreatedAtDescIdDesc(id);
        var status = DocumentStatus.of(history);
        boolean protectedFile =
                status.sent()
                        || status.unknown()
                        || history.stream()
                                .anyMatch(
                                        h ->
                                                h.getKind() == DocumentDeliveryKind.ISSUE
                                                        && h.getState()
                                                                == DocumentDeliveryState
                                                                        .PROCESSING);
        return new DocumentDetail(
                id,
                mapper.data(d),
                d.isCancelled(),
                d.getCancelledAt(),
                d.getFilename() != null,
                d.getChecksum(),
                d.getRevision(),
                d.getError(),
                d.getTo(),
                mapper.addresses(d.getCcJson()),
                !d.isCancelled() && !protectedFile && d.getSettlementId() != null,
                !protectedFile && d.getFilename() != null,
                !d.isCancelled() && !status.busy() && d.getFilename() != null,
                d.isCancelled() && !status.busy() && (status.sent() || status.unknown()),
                status.label(d),
                history.stream().map(mapper::history).toList(),
                d.getSettlementId(),
                d.getLeaseId());
    }

    DocumentPage list(
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
        var filter =
                new DocumentFilter(
                        users.getLoggedUser().id(),
                        tab,
                        type,
                        search,
                        apartmentId,
                        mailState,
                        from,
                        to,
                        settlementId,
                        leaseId,
                        tenantId);
        var result =
                documents.findAll(
                        filter.specification(),
                        PageRequest.of(
                                page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        var ids = result.getContent().stream().map(RentalDocument::getId).toList();
        Map<UUID, List<DocumentDelivery>> history =
                ids.isEmpty()
                        ? Map.of()
                        : deliveries
                                .findByDocumentIdInAndKindOrderByCreatedAtDescIdDesc(
                                        ids, DocumentDeliveryKind.ISSUE)
                                .stream()
                                .collect(Collectors.groupingBy(DocumentDelivery::getDocumentId));
        return new DocumentPage(
                result.getContent().stream()
                        .map(
                                d -> {
                                    var status =
                                            DocumentStatus.of(
                                                    history.getOrDefault(d.getId(), List.of()));
                                    return new DocumentItem(
                                            d.getId(),
                                            mapper.data(d),
                                            status.label(d),
                                            d.isCancelled(),
                                            d.getFilename() != null,
                                            status.latest(),
                                            status.lastSent());
                                })
                        .toList(),
                result.getTotalElements());
    }

    List<DocumentApartment> apartments() {
        return documents.apartments(users.getLoggedUser().id()).stream()
                .map(p -> new DocumentApartment(p.getId(), p.getName()))
                .toList();
    }

    Map<Long, DocumentSummary> settlementSummaries(Collection<Long> settlementIds) {
        if (settlementIds.isEmpty()) return Map.of();
        var rows = documents.findByUserIdAndSettlementIdIn(users.getLoggedUser().id(), settlementIds);
        var ids = rows.stream().map(RentalDocument::getId).toList();
        Map<UUID, List<DocumentDelivery>> history = ids.isEmpty() ? Map.of() : deliveries
                .findByDocumentIdInAndKindOrderByCreatedAtDescIdDesc(ids, DocumentDeliveryKind.ISSUE)
                .stream().collect(Collectors.groupingBy(DocumentDelivery::getDocumentId));
        return rows.stream().collect(Collectors.toMap(RentalDocument::getSettlementId, document -> {
            var state = DocumentStatus.of(history.getOrDefault(document.getId(), List.of()));
            String label = !document.isCancelled() && !state.sent() && !state.unknown()
                    && "FAILED".equals(state.latest()) ? "Błąd wysyłki" : state.label(document);
            return new DocumentSummary(document.getId(), document.getNumber(), label,
                    document.getFilename() != null, state.latest());
        }));
    }
}
