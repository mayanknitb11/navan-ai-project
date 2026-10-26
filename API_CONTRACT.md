# API Contract — Navan Commerce (transactions & invoices)

Base URL (dev): `http://localhost:8089` — every response below is a **real captured response** from the running service.

## Conventions

| Rule | Detail |
|---|---|
| Wire format | JSON, **snake_case** keys. Unknown keys are rejected (400), never silently dropped. |
| Alias | `POST/PATCH /invoices` also accepts camelCase `transactionIds` for the id list. No other camelCase keys are accepted anywhere. |
| Ids | Server-generated: `txn_<uuid>`, `inv_<uuid>`. |
| Money | Decimal, scale 2, HALF_EVEN (`220.00`). Range ±99,999,999,999,999,999.99 — beyond that → 400. No floats. |
| Dates | `occurred_at` / timestamps: ISO-8601 with time. Responses render in **IST** (`2026-03-11T12:10:00+05:30`); input may use any offset, `Z` included. Hotel `check_in`/`check_out`: `YYYY-MM-DD`. |
| Enums | `type`: `flight` \| `hotel` \| `rail` \| `navan_fee` (exact lowercase). `fee_kind`: `trip_fee` \| `agent_call_fee`. |
| Pagination | `limit` default 50, range 1–100; `offset` ≥ 0. `limit=1000` → 400. Never unbounded. |

### Error envelope (every error, same shape)

```json
{
  "status": "FAILED",
  "error_code": 409,
  "error_message": "A transaction with this external_id already exists",
  "code": "DUPLICATE_TRANSACTION",
  "existing_id": "txn_97ecd1b1-8b14-49de-874e-fff223017184"
}
```

| `code` | HTTP | When | Extra keys |
|---|---|---|---|
| `VALIDATION_FAILED` | 400 | Missing/invalid fields, unknown fields, bad date/currency/amount range | `fields` |
| `UNKNOWN_TYPE` | 400 | `type` not one of the four canonical values | — |
| `UNKNOWN_FEE_KIND` | 400 | `metadata.fee_kind` not `trip_fee`/`agent_call_fee` | — |
| `TRANSACTION_NOT_FOUND` | 404 | transaction id missing or deleted | — |
| `INVOICE_NOT_FOUND` | 404 | invoice id missing or deleted | — |
| `DUPLICATE_TRANSACTION` | 409 | `external_id` already exists | `existing_id` |
| `DUPLICATE_TRANSACTION_IDS` | 409 | same id twice in one invoice request | — |
| `MIXED_CURRENCY` | 409 | invoice spans >1 currency | `currencies` |
| `TRANSACTION_INVOICED` | 409 | PATCH/DELETE of a transaction that is on an invoice | `transaction_id` |
| `CONCURRENT_MODIFICATION` | 409 | overlapping PATCH/DELETE loser — safe to retry | — |
| `INTERNAL_ERROR` | 500 | unexpected; generic message only | — |

---

## Transactions

### POST /transactions — create

One API for all four types. `metadata` shape must match `type`. Header + `tax_lines` + `fee_lines` + `metadata` persist atomically.

**Flight**

```bash
curl -X POST http://localhost:8089/transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "flight",
    "external_id": "flight-TXL-LHR-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T06:40:00Z",
    "total": 220.00,
    "tax_lines": [{"name": "Airport tax", "amount": 15.00}],
    "fee_lines": [{"name": "YQ", "amount": 25.00}],
    "metadata": {
      "origin": "TXL", "destination": "LHR", "fare": 180.00,
      "coupons": [{"carrier": "BA", "flight_number": "BA982", "from": "TXL", "to": "LHR",
                   "departure": "2026-03-11T07:00:00Z", "arrival": "2026-03-11T08:20:00Z"}]
    }
  }'
```

`201 Created`

```json
{
  "id": "txn_97ecd1b1-8b14-49de-874e-fff223017184",
  "type": "flight",
  "external_id": "flight-TXL-LHR-2026-03-11",
  "currency": "EUR",
  "occurred_at": "2026-03-11T12:10:00+05:30",
  "total": 220.00,
  "tax_lines": [{"name": "Airport tax", "amount": 15.00}],
  "fee_lines": [{"name": "YQ", "amount": 25.00}],
  "metadata": {
    "origin": "TXL", "destination": "LHR", "fare": 180.00,
    "coupons": [{"flight_number": "BA982", "carrier": "BA", "from": "TXL", "to": "LHR",
                 "departure": "2026-03-11T12:30:00+05:30", "arrival": "2026-03-11T13:50:00+05:30"}]
  },
  "created_at": "2026-10-26T09:37:27.910352+05:30",
  "updated_at": "2026-10-26T09:37:27.910352+05:30"
}
```

**Hotel** — `metadata`: `name`, `address{street,city,country}`, `check_in`, `check_out`, `nights`, `room_rate_per_night`, optional `extras`:

```bash
curl -X POST http://localhost:8089/transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "hotel", "external_id": "hotel-LON-2026-03-11", "currency": "EUR",
    "occurred_at": "2026-03-11T15:00:00Z", "total": 331.70,
    "tax_lines": [{"name": "VAT", "rate": 0.07, "amount": 21.70}],
    "fee_lines": [],
    "metadata": {
      "name": "Hotel Mitte London",
      "address": {"street": "12 Kingsway", "city": "London", "country": "GB"},
      "check_in": "2026-03-11", "check_out": "2026-03-13",
      "nights": 2, "room_rate_per_night": 140.00,
      "extras": [{"name": "Breakfast", "amount": 30.00}]
    }
  }'
```

`201 Created` — note `rate` is kept on tax lines, omitted when null:

```json
{
  "id": "txn_d0fdb3a0-6d77-40c1-bd1b-c9cc24a9b6a6",
  "type": "hotel",
  "external_id": "hotel-LON-2026-03-11",
  "currency": "EUR",
  "occurred_at": "2026-03-11T20:30:00+05:30",
  "total": 331.70,
  "tax_lines": [{"name": "VAT", "rate": 0.07, "amount": 21.70}],
  "fee_lines": [],
  "metadata": {"name": "Hotel Mitte London",
    "address": {"street": "12 Kingsway", "city": "London", "country": "GB"},
    "check_in": "2026-03-11", "check_out": "2026-03-13",
    "nights": 2, "room_rate_per_night": 140.00,
    "extras": [{"name": "Breakfast", "amount": 30.00}]},
  "created_at": "2026-10-26T09:37:37.560882+05:30",
  "updated_at": "2026-10-26T09:37:37.560882+05:30"
}
```

**Rail** — `metadata.legs[]`: `origin`, `destination`, `departure`, `arrival`, optional `train_number`, `class`. A change of train = two legs:

```bash
curl -X POST http://localhost:8089/transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "rail", "external_id": "rail-STP-PAR-2026-03-13", "currency": "EUR",
    "occurred_at": "2026-03-13T09:00:00Z", "total": 94.00,
    "tax_lines": [],
    "fee_lines": [{"name": "Booking fee", "amount": 4.00}],
    "metadata": {"legs": [
      {"origin": "London St Pancras", "destination": "Lille Europe",
       "departure": "2026-03-13T09:01:00Z", "arrival": "2026-03-13T11:26:00Z",
       "train_number": "EST9114", "class": "standard"},
      {"origin": "Lille Europe", "destination": "Paris Nord",
       "departure": "2026-03-13T12:20:00Z", "arrival": "2026-03-13T13:47:00Z",
       "train_number": "TGV5213", "class": "standard"}
    ]}
  }'
```

**Navan fee** — `metadata.fee_kind` is `trip_fee` or `agent_call_fee`; optional `description`, `related_external_id`:

```bash
curl -X POST http://localhost:8089/transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "navan_fee", "external_id": "fee-trip-2026-03-11", "currency": "EUR",
    "occurred_at": "2026-03-11T09:00:00Z", "total": 25.00,
    "tax_lines": [], "fee_lines": [],
    "metadata": {"fee_kind": "trip_fee", "description": "Trip management fee",
                 "related_external_id": "flight-TXL-LHR-2026-03-11"}
  }'
```

`201 Created`

```json
{
  "id": "txn_061c7af8-cd04-49f3-98df-602aec585fa5",
  "type": "navan_fee",
  "external_id": "fee-trip-2026-03-11",
  "currency": "EUR",
  "occurred_at": "2026-03-11T14:30:00+05:30",
  "total": 25.00,
  "tax_lines": [],
  "fee_lines": [],
  "metadata": {"fee_kind": "trip_fee", "description": "Trip management fee",
               "related_external_id": "flight-TXL-LHR-2026-03-11"},
  "created_at": "2026-10-26T09:37:37.652397+05:30",
  "updated_at": "2026-10-26T09:37:37.652397+05:30"
}
```

**Duplicate `external_id`** — same body posted twice stores exactly one row; the second call returns `409` with the existing id (safe retry semantics):

```json
{"status": "FAILED", "error_code": 409,
 "error_message": "A transaction with this external_id already exists",
 "code": "DUPLICATE_TRANSACTION",
 "existing_id": "txn_97ecd1b1-8b14-49de-874e-fff223017184"}
```

### GET /transactions/{id}

```bash
curl http://localhost:8089/transactions/txn_97ecd1b1-8b14-49de-874e-fff223017184
```

`200` — identical shape to the create response above. Missing/deleted → `404`:

```json
{"status": "FAILED", "error_code": 404,
 "error_message": "Transaction not found: txn_missing",
 "code": "TRANSACTION_NOT_FOUND"}
```

### GET /transactions — paginated list

```bash
curl 'http://localhost:8089/transactions?limit=1&offset=0'
```

`200`

```json
{"items": [{"id": "txn_062b8aed-bfcd-4228-a224-21c7be6cd0e4", "type": "flight",
            "external_id": "flight-TXL-LHR-2026-03-11", "currency": "EUR",
            "occurred_at": "2026-03-11T12:10:00+05:30", "total": 220.00, "…": "…"}],
 "limit": 1, "offset": 0, "total": 27}
```

Out-of-range params → `400`:

```json
{"status": "FAILED", "error_code": 400,
 "error_message": "Validation failed: list.limit must be less than or equal to 100",
 "code": "VALIDATION_FAILED",
 "fields": [{"message": "must be less than or equal to 100", "field": "list.limit"}]}
```

### PATCH /transactions/{id}

All fields optional; present fields replace stored values. `tax_lines`/`fee_lines` are per-category **replace-all**. `external_id` and `type` are **not** patchable (unknown-field 400). `metadata` must match the stored type. **409 `TRANSACTION_INVOICED` if the transaction is on any invoice** — nothing is persisted.

```bash
curl -X PATCH http://localhost:8089/transactions/txn_d0fdb3a0-6d77-40c1-bd1b-c9cc24a9b6a6 \
  -H 'Content-Type: application/json' -d '{"total": 341.70}'
```

`200` — full updated record (note `updated_at` moved):

```json
{"id": "txn_d0fdb3a0-6d77-40c1-bd1b-c9cc24a9b6a6", "type": "hotel",
 "external_id": "hotel-LON-2026-03-11", "currency": "EUR",
 "occurred_at": "2026-03-11T20:30:00+05:30", "total": 341.70,
 "tax_lines": [{"name": "VAT", "rate": 0.07, "amount": 21.70}], "fee_lines": [],
 "metadata": {"…": "unchanged"},
 "created_at": "2026-10-26T09:37:37.560882+05:30", "updated_at": "2026-10-26T09:37:37.691276+05:30"}
```

Invoiced → `409`:

```json
{"status": "FAILED", "error_code": 409,
 "error_message": "Transaction is part of an invoice and cannot be modified or deleted",
 "code": "TRANSACTION_INVOICED",
 "transaction_id": "txn_97ecd1b1-8b14-49de-874e-fff223017184"}
```

### DELETE /transactions/{id}

`204 No Content` (soft delete; id stops resolving, `external_id` becomes reusable). On an invoice → `409 TRANSACTION_INVOICED`, nothing deleted — **unless** you pass `?force=true`, which skips the invoiced-guard and deletes anyway. The invoice is never impacted either way: it holds a frozen snapshot (physical copies, no live reference back to the transaction). Missing → `404`.

```bash
curl -X DELETE http://localhost:8089/transactions/txn_061c7af8-cd04-49f3-98df-602aec585fa5
curl -X DELETE 'http://localhost:8089/transactions/txn_061c7af8-cd04-49f3-98df-602aec585fa5?force=true'  # bypass the invoiced guard
```

---

## Invoices

### POST /invoices — create from transaction ids

Body: `{"transaction_ids": ["txn_…", …]}` (camelCase `transactionIds` accepted as alias). A single-id invoice is a one-element list. The invoice **deep-copies** each transaction (totals, lines, metadata) — later edits never change it.

```bash
curl -X POST http://localhost:8089/invoices \
  -H 'Content-Type: application/json' \
  -d '{"transaction_ids": ["txn_97ecd1b1-8b14-49de-874e-fff223017184"]}'
```

`201 Created`

```json
{
  "id": "inv_b7f78c2b-e1b8-4b6c-b28c-ea3d0f0bc330",
  "currency": "EUR",
  "total": 220.00,
  "line_count": 1,
  "lines": [{
    "transaction_id": "txn_97ecd1b1-8b14-49de-874e-fff223017184",
    "type": "flight",
    "external_id": "flight-TXL-LHR-2026-03-11",
    "currency": "EUR",
    "occurred_at": "2026-03-11T12:10:00+05:30",
    "total": 220.00,
    "tax_lines": [{"name": "Airport tax", "amount": 15.00}],
    "fee_lines": [{"name": "YQ", "amount": 25.00}],
    "metadata": {"fare": 180.00, "origin": "TXL", "destination": "LHR", "coupons": ["…"]}
  }],
  "tax_rollup": [{"name": "Airport tax", "amount": 15.00}],
  "fee_rollup": [{"name": "YQ", "amount": 25.00}],
  "created_at": "2026-10-26T09:37:28.081529+05:30",
  "updated_at": "2026-10-26T09:37:28.081529+05:30"
}
```

Failure modes: empty/absent list → `400`; any id missing → `404`; same id twice → `409 DUPLICATE_TRANSACTION_IDS`; mixed currencies → `409 MIXED_CURRENCY` (invoice not created).

### GET /invoices/{id}

`200` — same shape as create response (header + frozen line snapshots + rollups + totals). Missing/deleted → `404 INVOICE_NOT_FOUND`.

```bash
curl http://localhost:8089/invoices/inv_b7f78c2b-e1b8-4b6c-b28c-ea3d0f0bc330
```

### GET /invoices — paginated list

Same rules as `/transactions` (`limit` 1–100 default 50, `offset` ≥ 0):

```bash
curl 'http://localhost:8089/invoices?limit=50&offset=0'
```

`200` → `{"items": [ …invoice objects… ], "limit": 50, "offset": 0, "total": 3}`

### PATCH /invoices/{id}

Same body as create. **Replaces** the transaction-id set and rebuilds the snapshot from the **current** stored transactions in one atomic write. Any 404/409 leaves the existing snapshot untouched.

```bash
curl -X PATCH http://localhost:8089/invoices/inv_b7f78c2b-e1b8-4b6c-b28c-ea3d0f0bc330 \
  -H 'Content-Type: application/json' \
  -d '{"transaction_ids": ["txn_97ecd1b1-8b14-49de-874e-fff223017184", "txn_d0fdb3a0-6d77-40c1-bd1b-c9cc24a9b6a6"]}'
```

`200` — full rebuilt invoice. Mixed currency → `409 MIXED_CURRENCY`, invoice unchanged.

### DELETE /invoices/{id}

`204 No Content`. Deletes the invoice and its snapshot lines; the underlying transactions are untouched and become editable again. Missing → `404`.

```bash
curl -X DELETE http://localhost:8089/invoices/inv_b7f78c2b-e1b8-4b6c-b28c-ea3d0f0bc330
```

---

## Health

```bash
curl http://localhost:8089/health
```

`200` → `{"status": "UP"}`

## Client-behavior cheat sheet

- **Retry a create after any failure?** Safe — a duplicate `external_id` returns `409` + `existing_id`, never a second row (even under concurrency).
- **Need to change a billed transaction?** You can't (409). Delete/recreate is not possible either while it's on an invoice — delete the invoice first (transactions survive), then edit, then re-invoice.
- **`CONCURRENT_MODIFICATION` (409)** is the only retryable 409 — someone else wrote the same resource; re-read and retry.
- **400s are never retryable as-is** — fix the body (`fields[]` names the offenders).
