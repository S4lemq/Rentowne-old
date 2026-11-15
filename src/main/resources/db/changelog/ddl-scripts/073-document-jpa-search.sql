--liquibase formatted sql
--changeset rentowne:073-document-jpa-search
ALTER TABLE rental_document ADD COLUMN issue_date DATE;
ALTER TABLE rental_document ADD COLUMN tenant_name TEXT;
ALTER TABLE rental_document ADD COLUMN property_address TEXT;
UPDATE rental_document SET issue_date=(snapshot::jsonb->>'issueDate')::date,
    tenant_name=snapshot::jsonb->'tenant'->>'name',property_address=snapshot::jsonb->>'propertyAddress';
ALTER TABLE rental_document ALTER COLUMN issue_date SET NOT NULL;
ALTER TABLE rental_document ALTER COLUMN tenant_name SET NOT NULL;
ALTER TABLE rental_document ALTER COLUMN property_address SET NOT NULL;
CREATE INDEX i_document_issue_date ON rental_document(user_account_id,issue_date);
