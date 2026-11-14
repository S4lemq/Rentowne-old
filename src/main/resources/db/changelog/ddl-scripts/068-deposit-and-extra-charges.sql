--liquibase formatted sql

--changeset rentowne:068-deposit-and-extra-charges
ALTER TABLE lease_agreement ALTER COLUMN deposit_paid DROP NOT NULL;
ALTER TABLE lease_agreement ADD COLUMN deposit_opening_paid NUMERIC(14,2);
ALTER TABLE lease_agreement ADD COLUMN deposit_balance_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
-- Historical numbers are retained, but never taken as proof of unpaid deposit.
UPDATE lease_agreement SET deposit_opening_paid = deposit_paid;

ALTER TABLE rental_payment ADD COLUMN deposit_amount NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE rental_payment ADD CONSTRAINT ck_payment_deposit CHECK (deposit_amount >= 0 AND deposit_amount <= amount);

ALTER TABLE settlement ADD COLUMN deposit_top_up NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE settlement ADD COLUMN deposit_carry_in NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE settlement ADD COLUMN deposit_assessment BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE settlement ADD COLUMN additional_charge NUMERIC(14,2) NOT NULL DEFAULT 0;
ALTER TABLE settlement ADD COLUMN additional_charge_description VARCHAR(500);
CREATE UNIQUE INDEX uk_settlement_deposit_assessment ON settlement(tenant_id) WHERE deposit_assessment = TRUE;

-- Keep a tombstone on deletion: a delayed retry must not recreate a cancelled charge.
CREATE TABLE settlement_operation (
    tenant_id BIGINT NOT NULL REFERENCES tenant(tenant_id),
    request_id UUID NOT NULL,
    payload TEXT NOT NULL,
    settlement_id BIGINT REFERENCES settlement(settlement_id) ON DELETE SET NULL,
    PRIMARY KEY (tenant_id, request_id)
);
