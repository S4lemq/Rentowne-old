--liquibase formatted sql
--changeset rentowne:072-manual-document-delivery
ALTER TABLE rental_document ADD COLUMN cancelled_at TIMESTAMPTZ;
ALTER TABLE rental_document ADD COLUMN generated_at TIMESTAMPTZ;
ALTER TABLE rental_document ADD COLUMN generation_error VARCHAR(1000);
ALTER TABLE rental_document ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE rental_document ADD COLUMN request_json TEXT;
ALTER TABLE rental_document ADD COLUMN apartment_id BIGINT;
ALTER TABLE rental_document ADD COLUMN rented_object_id BIGINT;
ALTER TABLE rental_document ADD COLUMN to_address VARCHAR(254);
ALTER TABLE rental_document ADD COLUMN cc_json TEXT NOT NULL DEFAULT '[]';
UPDATE rental_document SET cancelled_at=created_at WHERE cancelled;
UPDATE rental_document d SET request_json=substring(o.payload FROM position('|' IN o.payload)+1)
FROM settlement_operation o WHERE o.settlement_id=d.settlement_id AND position('|' IN o.payload)>0;
UPDATE rental_document d SET apartment_id=r.apartment_id,rented_object_id=r.rented_object_id
FROM tenant t JOIN rented_object r ON r.rented_object_id=t.rented_object_id WHERE t.tenant_id=d.tenant_id;
UPDATE rental_document d SET to_address=m.to_address,cc_json=m.cc_json
FROM document_delivery m WHERE m.document_id=d.document_id AND m.kind='ISSUE';
UPDATE document_delivery SET state=CASE WHEN state='PROCESSING' AND smtp_started_at IS NOT NULL
    THEN 'UNKNOWN' ELSE 'CANCELLED' END,finished_at=now(),lease_token=NULL,lease_until=NULL
WHERE state IN ('PENDING','PROCESSING','HELD');
UPDATE document_delivery_attempt a SET outcome=d.state,finished_at=now()
FROM document_delivery d WHERE a.delivery_id=d.delivery_id AND a.outcome='PROCESSING';
ALTER TABLE document_delivery DROP CONSTRAINT uk_document_delivery_kind;
ALTER TABLE document_delivery ADD COLUMN request_id UUID;
ALTER TABLE document_delivery ADD COLUMN requested_by BIGINT;
ALTER TABLE document_delivery ADD COLUMN document_revision BIGINT;
ALTER TABLE document_delivery ADD COLUMN attachment_sha256 VARCHAR(64);
CREATE UNIQUE INDEX uk_document_manual_request ON document_delivery(document_id,request_id);
CREATE UNIQUE INDEX uk_document_active_send ON document_delivery(document_id) WHERE state='PROCESSING';
CREATE INDEX i_document_filter ON rental_document(user_account_id,cancelled,created_at);
