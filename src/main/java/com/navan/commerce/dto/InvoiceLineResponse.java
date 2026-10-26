package com.navan.commerce.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Frozen snapshot of one transaction on an invoice. {@code metadata} is the stored JSON blob as-is —
 * a snapshot is opaque by design and is never reinterpreted against current transaction state.
 */
@Getter
@Builder
public class InvoiceLineResponse {

    private final String transactionId;
    private final String type;
    private final String externalId;
    private final String currency;
    private final Instant occurredAt;
    private final BigDecimal total;
    private final List<LineEntry> taxLines;
    private final List<LineEntry> feeLines;
    private final JsonNode metadata;
}
