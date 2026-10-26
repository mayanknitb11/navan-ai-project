# DB SCHEMA — Navan Commerce Transactions & Invoices

> PostgreSQL 16, Flyway-managed (`V1__init.sql` mirrors this file — the single source of truth;
> Hibernate runs `ddl-auto=validate`). Decisions reference `ARCHITECTURE.md` (D1–D13).

---

## 1. Tables & Indexes (DDL)

```sql
-- Every table carries the shared BaseEntity columns:
--   id VARCHAR(40) PK (assigned — prefixed UUIDs on roots, plain UUIDs on lines),
--   version BIGINT (optimistic lock), created_at / updated_at TIMESTAMPTZ.

CREATE TABLE transactions (
    id            VARCHAR(40)    NOT NULL,
    external_id   VARCHAR(255)   NOT NULL,           -- client identity; unique among ACTIVE rows
    type          VARCHAR(16)    NOT NULL,
    currency      VARCHAR(3)     NOT NULL,
    occurred_at   TIMESTAMPTZ    NOT NULL,
    total         NUMERIC(19, 2) NOT NULL,
    metadata      JSONB          NOT NULL,           -- type-specific blob (flight coupons, hotel address, …)
    version       BIGINT         NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ    NOT NULL,
    updated_at    TIMESTAMPTZ    NOT NULL,
    deleted_at    TIMESTAMPTZ    NULL,               -- NULL = active (soft delete)
    PRIMARY KEY (id),
    CONSTRAINT chk_transactions_type CHECK (type IN ('flight', 'hotel', 'rail', 'navan_fee'))
);

-- Duplicate guard: exactly ONE active row per external_id, DB-enforced under concurrent POSTs.
-- Partial, so a soft-deleted external_id can be re-created (deleted = does not exist).
CREATE UNIQUE INDEX uk_transactions_active_external_id
    ON transactions (external_id) WHERE deleted_at IS NULL;

-- List pagination: ORDER BY created_at, id LIMIT/OFFSET over active rows only.
CREATE INDEX idx_transactions_created_id
    ON transactions (created_at, id) WHERE deleted_at IS NULL;

-- All money lines (tax/fee/…), discriminated by `category`. Shared fields are real columns
-- (name, amount); category-specific fields (tax rate as {"rate": …}) live in `attributes`.
-- No CHECK on category — a new category must be one enum value in code, not a migration.
CREATE TABLE transaction_lines (
    id             VARCHAR(40)    NOT NULL,
    transaction_id VARCHAR(40)    NOT NULL,
    category       VARCHAR(16)    NOT NULL,          -- tax | fee | …
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

-- Snapshot lines: physical copies of the billed transactions. Reads NEVER join `transactions`.
CREATE TABLE invoice_lines (
    id                VARCHAR(40)    NOT NULL,
    invoice_id        VARCHAR(40)    NOT NULL,
    transaction_id    VARCHAR(40)    NOT NULL,       -- no FK by design: invoiced-guard lookup only
    type              VARCHAR(16)    NOT NULL,
    external_id       VARCHAR(255)   NOT NULL,
    occurred_at       TIMESTAMPTZ    NOT NULL,
    total             NUMERIC(19, 2) NOT NULL,
    metadata_snapshot JSONB          NOT NULL,
    breakdown         JSONB          NOT NULL,       -- frozen {"tax_lines": [...], "fee_lines": [...]}
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL,
    updated_at        TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_line_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices (id) ON DELETE CASCADE
);

CREATE INDEX idx_invoice_lines_invoice ON invoice_lines (invoice_id);
-- Powers "is this transaction on any invoice?" (PATCH/DELETE guard):
CREATE INDEX idx_invoice_lines_txn     ON invoice_lines (transaction_id);
```

---

## 2. Design Decisions

1. **Identity in the database, not in code (D1/D3).** The partial unique index on `external_id` is the only duplicate guard — no check-then-act; the loser of a concurrent POST race hits the constraint and reads back the winner's id → 409 with `existing_id`. Rejected: plain `UNIQUE(external_id)` (blocks re-creation after delete forever) and an `active` flag column (`deleted_at` *is* the flag and records when — the first audit question on financial data).
2. **One category-discriminated lines table for taxes & fees** (`transaction_lines`). The API still exposes separate `tax_lines`/`fee_lines` (mappers split by `category`); a new line category = one enum value, a new category-specific field = a key in `attributes` JSONB — zero DDL.
3. **Invoice = frozen physical snapshot (D2/D9).** `invoice_lines` duplicates the transaction shape on purpose; the tax/fee split rides as `breakdown` JSONB (typed in code via `LineBreakdownConverter`) because a snapshot is write-once/read-whole — relational child rows would buy nothing. Accepted tradeoff: cross-invoice tax analytics would need JSONB unrolling or a reporting pipeline, never the OLTP path.
4. **No FK from `invoice_lines.transaction_id` → `transactions`.** It exists solely for the invoiced-guard `SELECT EXISTS`; a hard FK would couple snapshot lifecycle to transaction lifecycle without enforcing anything we need.
5. **Soft delete on aggregate roots only** (`transactions`, `invoices`). Children have no `deleted_at` — reachable only through the parent. DELETE = one versioned UPDATE (`setDeletedAt` + `@Version`); nothing financial is ever physically destroyed. All reads filter `deleted_at IS NULL` at the DAO layer (single query-composition point) — matching the partial indexes, so deleted rows neither leak nor bloat lists.
6. **`metadata` is JSONB by choice (D2).** Round-tripped but never queried by field; the sealed Java model keeps it typed. Everything the spec demands as real columns/tables (identity, taxes, fees) is one.
7. **Concurrency:** `@Version` on every table; overlapping PATCH/DELETE loser → 409 `CONCURRENT_MODIFICATION`. Every write is one `@Transactional` boundary — header + lines + metadata are all-or-nothing.
8. **Assigned ids + `Persistable.isNew()`** keep creates as plain INSERTs (no merge SELECT) — which the duplicate-race path depends on.
9. **Ordering:** line lists are unordered relational sets; JSON arrays in `metadata` (coupons, legs, extras) preserve client order. Rollups/totals are order-independent.

---

## 3. Money & Time

| Concern | Column | Java | Rule |
|---|---|---|---|
| Amounts (all `total`/`amount`) | `NUMERIC(19,2)` | `BigDecimal` | Scale 2, HALF_EVEN via `MoneyUtils`; no float/double anywhere. Request DTOs bean-validate the 19,2 range (`@DecimalMin`/`@DecimalMax`) so overflow is a 400, never a DB error surfacing as a bogus 409. |
| Tax `rate` | `transaction_lines.attributes` JSONB | `BigDecimal` | Optional; absent = one rollup bucket keyed `(name, NULL)`. |
| Instants (`occurred_at`, audit) | `TIMESTAMPTZ` | `Instant` | Absolute time; JDBC session in Asia/Kolkata (`hibernate.jdbc.time_zone`); rendered on the wire in IST (`+05:30`) via `JacksonConfig` — any ISO-8601 offset still accepted on input. |
| Hotel `check_in`/`check_out` | inside `metadata` JSONB | `LocalDate` | Date-only by domain meaning. |

**Limitation (D6):** scale-2 assumes 2-minor-unit currencies (fixtures are EUR). Production extension = a currency table carrying minor units; the amount column widens without schema reshape.
