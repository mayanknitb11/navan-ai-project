-- V1__init.sql — mirrors DB_SCHEMA.md (PostgreSQL 16).
-- Hibernate runs with ddl-auto=validate; this file is the single source of truth for the schema.
-- Every table carries the BaseEntity columns: id VARCHAR(40) PK (assigned), version, created_at, updated_at.

CREATE TABLE transactions (
    id            VARCHAR(40)    NOT NULL,
    external_id   VARCHAR(255)   NOT NULL,
    type          VARCHAR(16)    NOT NULL,
    currency      VARCHAR(3)     NOT NULL,
    occurred_at   TIMESTAMPTZ    NOT NULL,
    total         NUMERIC(19, 2) NOT NULL,
    metadata      JSONB          NOT NULL,
    version       BIGINT         NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ    NOT NULL,
    updated_at    TIMESTAMPTZ    NOT NULL,
    deleted_at    TIMESTAMPTZ    NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_transactions_type CHECK (type IN ('flight', 'hotel', 'rail', 'navan_fee'))
);

-- Partial unique index: exactly ONE active row per external_id, enforced by the database
-- including under concurrent POSTs. Soft-deleted rows fall outside the index predicate,
-- so a deleted external_id can be re-created (with a new id).
CREATE UNIQUE INDEX uk_transactions_active_external_id
    ON transactions (external_id) WHERE deleted_at IS NULL;

-- Pagination: ORDER BY created_at, id with LIMIT/OFFSET. Partial so soft-deleted rows
-- never bloat the list path (every DAO query carries the same predicate).
CREATE INDEX idx_transactions_created_id
    ON transactions (created_at, id) WHERE deleted_at IS NULL;

-- Merged money-lines table (tax/fee/…), discriminated by category. Fields common to every
-- category are real typed columns (name, amount); category-specific fields (the tax rate
-- today, as {"rate": …}) live in `attributes` JSONB: a new field lands there with zero
-- migration; a new category is one enum value in code (no CHECK here on purpose — extension
-- must not need DDL).
CREATE TABLE transaction_lines (
    id             VARCHAR(40)    NOT NULL,
    transaction_id VARCHAR(40)    NOT NULL,
    category       VARCHAR(16)    NOT NULL,
    name           VARCHAR(128)   NOT NULL,
    amount         NUMERIC(19, 2) NOT NULL,
    attributes     JSONB          NULL,
    version        BIGINT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ    NOT NULL,
    updated_at     TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tl_txn FOREIGN KEY (transaction_id)
        REFERENCES transactions (id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_lines_txn ON transaction_lines (transaction_id);

CREATE TABLE invoices (
    id          VARCHAR(40)    NOT NULL,
    currency    VARCHAR(3)     NOT NULL,
    total       NUMERIC(19, 2) NOT NULL,
    line_count  INT            NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL,
    deleted_at  TIMESTAMPTZ    NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_invoices_created_id
    ON invoices (created_at, id) WHERE deleted_at IS NULL;

CREATE TABLE invoice_lines (
    id                VARCHAR(40)    NOT NULL,
    invoice_id        VARCHAR(40)    NOT NULL,
    -- No FK to transactions by design (DB_SCHEMA rule 6): reference for the
    -- invoiced-guard only, never used to source invoice data.
    transaction_id    VARCHAR(40)    NOT NULL,
    type              VARCHAR(16)    NOT NULL,
    external_id       VARCHAR(255)   NOT NULL,
    occurred_at       TIMESTAMPTZ    NOT NULL,
    total             NUMERIC(19, 2) NOT NULL,
    metadata_snapshot JSONB          NOT NULL,
    -- Frozen tax/fee split of this billed item: {"tax_lines": [...], "fee_lines": [...]}.
    -- JSONB because an invoice snapshot is write-once / read-whole and immutable by rule —
    -- relational child rows would buy nothing here (DB_SCHEMA §2 rule 4). Typed in code as
    -- List<LineItem> via LineBreakdownConverter; older snapshots keep their written shape.
    breakdown         JSONB          NOT NULL,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_line_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices (id) ON DELETE CASCADE
);

CREATE INDEX idx_invoice_lines_invoice ON invoice_lines (invoice_id);
-- Powers "is this transaction on any invoice?" for the PATCH/DELETE guard.
CREATE INDEX idx_invoice_lines_txn ON invoice_lines (transaction_id);
