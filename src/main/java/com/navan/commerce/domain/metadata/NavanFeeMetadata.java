package com.navan.commerce.domain.metadata;

import com.navan.commerce.domain.FeeKind;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/** Navan-owned charge on a trip (e.g. trip_fee, agent_call_fee) — a first-class transaction. */
@Getter
@Builder
@Jacksonized
public final class NavanFeeMetadata implements TransactionMetadata {

    @NotNull
    private final FeeKind feeKind;

    private final String description;

    private final String relatedExternalId;
}
