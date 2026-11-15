package pl.rentowne.rental_document;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DocumentDelivery {
    @Id
    @Column(name = "delivery_id")
    private UUID id;

    @Column(name = "document_id")
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind")
    private DocumentDeliveryKind kind;

    @Column(name = "to_address")
    private String to;

    @Column(name = "cc_json")
    private String ccJson;

    @Column(name = "send_enabled")
    private boolean sendEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private DocumentDeliveryState state;

    @Column(name = "attempt_count")
    private int attempts;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "smtp_started_at")
    private OffsetDateTime smtpStartedAt;

    @Column(name = "last_error")
    private String error;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "document_revision")
    private Long documentRevision;

    @Column(name = "attachment_sha256")
    private String checksum;

    static DocumentDelivery start(
            RentalDocument document,
            DocumentSendRequest request,
            Long userId,
            String to,
            String ccJson) {
        var delivery = new DocumentDelivery();
        delivery.id = UUID.randomUUID();
        delivery.documentId = document.getId();
        delivery.kind =
                request.cancellation()
                        ? DocumentDeliveryKind.CANCELLATION
                        : DocumentDeliveryKind.ISSUE;
        delivery.to = to;
        delivery.ccJson = ccJson;
        delivery.sendEnabled = true;
        delivery.state = DocumentDeliveryState.PROCESSING;
        delivery.attempts = 1;
        delivery.createdAt = OffsetDateTime.now();
        delivery.nextAttemptAt = delivery.createdAt;
        delivery.smtpStartedAt = delivery.createdAt;
        delivery.requestId = request.requestId();
        delivery.requestedBy = userId;
        delivery.documentRevision = document.getRevision();
        delivery.checksum = document.getChecksum();
        return delivery;
    }

    void finish(DocumentDeliveryState outcome, String error) {
        if (state == DocumentDeliveryState.PROCESSING) {
            state = outcome;
            this.error = error;
            finishedAt = OffsetDateTime.now();
        }
    }
}
