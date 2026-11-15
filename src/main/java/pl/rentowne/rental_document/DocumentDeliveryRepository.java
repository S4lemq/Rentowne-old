package pl.rentowne.rental_document;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.*;

interface DocumentDeliveryRepository extends JpaRepository<DocumentDelivery, UUID> {
    boolean existsByDocumentIdAndRequestId(UUID documentId, UUID requestId);

    List<DocumentDelivery> findByDocumentIdOrderByCreatedAtDescIdDesc(UUID documentId);

    List<DocumentDelivery> findByDocumentIdInAndKindOrderByCreatedAtDescIdDesc(
            Collection<UUID> ids, DocumentDeliveryKind kind);

    boolean existsByDocumentIdAndKindAndStateIn(
            UUID id, DocumentDeliveryKind kind, Collection<DocumentDeliveryState> states);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
update DocumentDelivery d set d.state=pl.rentowne.rental_document.DocumentDeliveryState.UNKNOWN,
d.finishedAt=:now,d.error=:error where d.state=pl.rentowne.rental_document.DocumentDeliveryState.PROCESSING
""")
    int recoverInterrupted(@Param("now") OffsetDateTime now, @Param("error") String error);

    default boolean protectsFile(UUID id) {
        return existsByDocumentIdAndKindAndStateIn(
                id,
                DocumentDeliveryKind.ISSUE,
                List.of(
                        DocumentDeliveryState.SENT,
                        DocumentDeliveryState.UNKNOWN,
                        DocumentDeliveryState.PROCESSING));
    }
}
