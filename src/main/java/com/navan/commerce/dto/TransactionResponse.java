package com.navan.commerce.dto;

import com.navan.commerce.domain.metadata.TransactionMetadata;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Full transaction record, including typed metadata (spec §2: GET must round-trip the structure). */
@Getter
@Builder
public class TransactionResponse {

    private final String id;
    private final String type;
    private final String externalId;
    private final String currency;
    private final Instant occurredAt;
    private final BigDecimal total;
    private final List<LineEntry> taxLines;
    private final List<LineEntry> feeLines;
    private final TransactionMetadata metadata;
    private final Instant createdAt;
    private final Instant updatedAt;
}
