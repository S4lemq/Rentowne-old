package pl.rentowne.rental_document;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "rental_document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RentalDocument {
    @Id
    @Column(name = "document_id")
    private UUID id;

    @Column(name = "user_account_id")
    private Long userId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "settlement_id")
    private Long settlementId;

    @Column(name = "lease_agreement_id")
    private Long leaseId;

    @Column(name = "source_key")
    private String sourceKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private RentalDocumentType type;

    @Column(name = "document_year")
    private int year;

    @Column(name = "sequence_number")
    private int sequence;

    @Column(name = "document_number")
    private String number;

    @Column(name = "template_version")
    private int templateVersion = 1;

    @Column(name = "snapshot")
    private String snapshot;

    @Column(name = "pdf_file_name")
    private String filename;

    @Column(name = "pdf_sha256")
    private String checksum;

    @Column(name = "cancelled")
    private boolean cancelled;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    @Column(name = "generation_error")
    private String error;

    @Version
    @Column(name = "revision")
    private Long revision;

    @Column(name = "request_json")
    private String requestJson;

    @Column(name = "apartment_id")
    private Long apartmentId;

    @Column(name = "rented_object_id")
    private Long objectId;

    @Column(name = "to_address")
    private String to;

    @Column(name = "cc_json")
    private String ccJson;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "property_address")
    private String propertyAddress;

    static RentalDocument create(
            UUID id,
            Long userId,
            Long ownerId,
            Long tenantId,
            Long settlementId,
            Long leaseId,
            String source,
            int sequence,
            RentalDocumentData data,
            String snapshot,
            String requestJson,
            Long apartmentId,
            Long objectId,
            String to,
            String ccJson) {
        var document = new RentalDocument();
        document.id = id;
        document.userId = userId;
        document.ownerId = ownerId;
        document.tenantId = tenantId;
        document.settlementId = settlementId;
        document.leaseId = leaseId;
        document.sourceKey = source;
        document.type = data.type();
        document.year = data.issueDate().getYear();
        document.sequence = sequence;
        document.number = data.number();
        document.createdAt = OffsetDateTime.now();
        document.requestJson = requestJson;
        document.apartmentId = apartmentId;
        document.objectId = objectId;
        document.to = to;
        document.ccJson = ccJson;
        document.updateData(data, snapshot);
        return document;
    }

    void updateData(RentalDocumentData data, String snapshot) {
        this.snapshot = snapshot;
        issueDate = data.issueDate();
        tenantName = data.tenant().name();
        propertyAddress = data.propertyAddress();
    }

    void cancel() {
        if (!cancelled) {
            cancelled = true;
            cancelledAt = OffsetDateTime.now();
        }
    }

    void generated(RentalDocumentData data, String snapshot, String filename, String checksum) {
        updateData(data, snapshot);
        this.filename = filename;
        this.checksum = checksum;
        generatedAt = OffsetDateTime.now();
        error = null;
    }

    void removePdf() {
        filename = null;
        checksum = null;
        error = null;
    }

    void generationFailed(String message) {
        error = message;
    }

    void recipients(String to, String ccJson) {
        this.to = to;
        this.ccJson = ccJson;
    }
}
