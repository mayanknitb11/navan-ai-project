package com.navan.commerce.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * POST /invoices (and PATCH /invoices/{id}) body: {@code {"transaction_ids": ["txn_…", …]}}.
 * One list shape only — a single-id invoice is just a one-element list (spec §3: either shape
 * is fine, at least a single id must be supported).
 * The camelCase spelling {@code transactionIds} is accepted as an alias; the canonical wire
 * name stays snake_case. Any other unknown field is still rejected (400).
 * Absent/empty ids → 400, repeated ids → 409 (decision D4).
 */
@Getter
@Builder
@Jacksonized
public class CreateInvoiceRequest {

    @JsonAlias("transactionIds")
    private final List<String> transactionIds;
}
