package com.navan.commerce.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Invoice header + frozen line snapshots + tax/fee rollups (spec §3). */
@Getter
@Builder
public class InvoiceResponse {

    private final String id;
    private final String currency;
    private final BigDecimal total;
    private final int lineCount;
    private final List<InvoiceLineResponse> lines;
    private final List<LineEntry> taxRollup;
    private final List<LineEntry> feeRollup;
    private final Instant createdAt;
    private final Instant updatedAt;
}
