package pl.rentowne.rental_document;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC operations participate in the same Spring transaction as JPA financial changes. */
@Repository
@RequiredArgsConstructor
class DocumentStore {
    private final JdbcTemplate jdbc;

    record Stored(UUID id, String number, String snapshot, String filename, String checksum, boolean cancelled, int templateVersion) {}
    record Delivery(UUID id, UUID documentId, String kind, String to, String ccJson, boolean sendEnabled,
                    String state, int attempts, UUID token) {}

    boolean exists(String source) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM rental_document WHERE source_key=?)", Boolean.class, source));
    }

    int nextNumber(Long ownerId, RentalDocumentType type, int year) {
        return jdbc.queryForObject("""
                INSERT INTO rental_document_counter(owner_id,document_type,document_year,last_number) VALUES (?,?,?,1)
                ON CONFLICT (owner_id,document_type,document_year) DO UPDATE SET last_number=rental_document_counter.last_number+1
                RETURNING last_number
                """, Integer.class, ownerId, type.name(), year);
    }

    void insert(UUID id, Long userId, Long ownerId, Long tenantId, Long settlementId, Long leaseId,
                String source, RentalDocumentData data, int sequence, String snapshot) {
        jdbc.update("""
                INSERT INTO rental_document(document_id,user_account_id,owner_id,tenant_id,settlement_id,lease_agreement_id,
                source_key,document_type,document_year,sequence_number,document_number,snapshot)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, id, userId, ownerId, tenantId, settlementId, leaseId, source, data.type().name(),
                data.issueDate().getYear(), sequence, data.number(), snapshot);
    }

    void enqueue(UUID documentId, String kind, String to, String ccJson, boolean enabled) {
        jdbc.update("""
                INSERT INTO document_delivery(delivery_id,document_id,kind,to_address,cc_json,send_enabled)
                VALUES (?,?,?,?,?,?) ON CONFLICT (document_id,kind) DO NOTHING
                """, UUID.randomUUID(), documentId, kind, to, ccJson, enabled);
    }

    Stored document(UUID id, boolean lock) {
        return jdbc.queryForObject("SELECT * FROM rental_document WHERE document_id=?" + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> new Stored(id, rs.getString("document_number"), rs.getString("snapshot"),
                        rs.getString("pdf_file_name"), rs.getString("pdf_sha256"), rs.getBoolean("cancelled"), rs.getInt("template_version")), id);
    }

    Optional<Delivery> claim(DocumentProperties properties) {
        // A lost worker after starting SMTP has an unknown outcome and must not send again automatically.
        List<UUID> expired = jdbc.query("SELECT delivery_id FROM document_delivery WHERE state='PROCESSING' AND lease_until<now() FOR UPDATE SKIP LOCKED",
                (rs, row) -> rs.getObject(1, UUID.class));
        for (UUID id : expired) {
            jdbc.update("""
                    UPDATE document_delivery SET state=CASE WHEN smtp_started_at IS NOT NULL THEN 'UNKNOWN'
                    WHEN attempt_count>=? THEN 'FAILED' ELSE 'PENDING' END,
                    last_error='Przerwano wykonanie zadania; sprawdź wynik ostatniej próby.', lease_token=NULL,lease_until=NULL
                    WHERE delivery_id=?
                    """, properties.maxAttempts(), id);
            jdbc.update("""
                    UPDATE document_delivery_attempt SET outcome=(SELECT state FROM document_delivery WHERE delivery_id=?),
                    finished_at=now(),error_message='Przerwano wykonanie zadania.' WHERE delivery_id=? AND outcome='PROCESSING'
                    """, id, id);
        }
        List<UUID> ids = jdbc.query("""
                SELECT delivery_id FROM document_delivery WHERE state='PENDING' AND next_attempt_at<=now()
                ORDER BY next_attempt_at,created_at,delivery_id LIMIT 1 FOR UPDATE SKIP LOCKED
                """, (rs, row) -> rs.getObject(1, UUID.class));
        if (ids.isEmpty()) return Optional.empty();
        UUID id = ids.getFirst();
        UUID token = UUID.randomUUID();
        jdbc.update("""
                UPDATE document_delivery SET state='PROCESSING',attempt_count=attempt_count+1,lease_token=?,
                lease_until=now()+? * INTERVAL '1 second',smtp_started_at=NULL,finished_at=NULL WHERE delivery_id=?
                """, token, properties.processingLease().toSeconds(), id);
        Delivery delivery = delivery(id, false);
        jdbc.update("""
                INSERT INTO document_delivery_attempt(attempt_id,delivery_id,attempt_number,outcome) VALUES (?,?,?,'PROCESSING')
                """, UUID.randomUUID(), id, delivery.attempts());
        return Optional.of(delivery);
    }

    Delivery delivery(UUID id, boolean lock) {
        return jdbc.queryForObject("SELECT * FROM document_delivery WHERE delivery_id=?" + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> delivery(rs), id);
    }

    private Delivery delivery(ResultSet rs) throws SQLException {
        return new Delivery(rs.getObject("delivery_id", UUID.class), rs.getObject("document_id", UUID.class),
                rs.getString("kind"), rs.getString("to_address"), rs.getString("cc_json"), rs.getBoolean("send_enabled"),
                rs.getString("state"), rs.getInt("attempt_count"), rs.getObject("lease_token", UUID.class));
    }

    boolean owns(Delivery current, Delivery claimed) {
        return "PROCESSING".equals(current.state()) && claimed.token().equals(current.token());
    }

    void savePdf(UUID documentId, String filename, String checksum) {
        jdbc.update("UPDATE rental_document SET pdf_file_name=?,pdf_sha256=? WHERE document_id=? AND pdf_file_name IS NULL", filename, checksum, documentId);
    }

    boolean markSmtp(Delivery delivery) {
        return jdbc.update("UPDATE document_delivery SET smtp_started_at=now() WHERE delivery_id=? AND state='PROCESSING' AND lease_token=?",
                delivery.id(), delivery.token()) == 1;
    }

    void finish(Delivery delivery, String state, String error) {
        int delaySeconds = delivery.attempts() == 1 ? 60 : 300;
        int updated = jdbc.update("""
                UPDATE document_delivery SET state=?,last_error=?,finished_at=now(),lease_token=NULL,lease_until=NULL,
                next_attempt_at=now()+? * INTERVAL '1 second' WHERE delivery_id=? AND lease_token=?
                """, state, error, delaySeconds, delivery.id(), delivery.token());
        if (updated == 0) return;
        jdbc.update("""
                UPDATE document_delivery_attempt SET outcome=?,error_message=?,finished_at=now()
                WHERE delivery_id=? AND attempt_number=? AND outcome='PROCESSING'
                """, state, error, delivery.id(), delivery.attempts());
    }

    void cancelReceipt(Long settlementId) {
        List<UUID> ids = jdbc.query("SELECT document_id FROM rental_document WHERE settlement_id=? AND document_type='RENT_RECEIPT' FOR UPDATE",
                (rs, row) -> rs.getObject(1, UUID.class), settlementId);
        for (UUID id : ids) {
            Stored document = document(id, true);
            if (document.cancelled()) continue;
            jdbc.update("UPDATE rental_document SET cancelled=true WHERE document_id=?", id);
            List<Delivery> deliveries = jdbc.query("SELECT * FROM document_delivery WHERE document_id=? AND kind='ISSUE' FOR UPDATE",
                    (rs, row) -> delivery(rs), id);
            for (Delivery issue : deliveries) {
                // A crash can leave PROCESSING after SMTP accepted the message but before
                // its result was committed. Cancellation must preserve that uncertainty.
                boolean interruptedSmtp = "PROCESSING".equals(issue.state()) && Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT smtp_started_at IS NOT NULL FROM document_delivery WHERE delivery_id=?", Boolean.class, issue.id()));
                if (interruptedSmtp) {
                    finish(issue, "UNKNOWN", "Anulowano dokument bez potwierdzonego wyniku wysyłki SMTP.");
                }
                if (interruptedSmtp || List.of("SENT", "UNKNOWN").contains(issue.state())) {
                    enqueue(id, "CANCELLATION", issue.to(), issue.ccJson(), issue.sendEnabled());
                } else if (!"SIMULATED".equals(issue.state())) {
                    jdbc.update("UPDATE document_delivery SET state='CANCELLED',lease_token=NULL,lease_until=NULL,finished_at=now() WHERE delivery_id=?", issue.id());
                    jdbc.update("UPDATE document_delivery_attempt SET outcome='CANCELLED',finished_at=now() WHERE delivery_id=? AND outcome='PROCESSING'", issue.id());
                }
            }
        }
    }
}
