package com.navan.commerce.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.navan.commerce.constant.FeeKinds;
import lombok.Getter;

/**
 * Navan-owned fee kinds (spec §1: at least trip_fee and agent_call_fee). Adding a new kind is
 * one enum value — no other code changes. Unknown kinds surface as 400 UNKNOWN_FEE_KIND.
 */
@Getter
public enum FeeKind {

    TRIP_FEE(FeeKinds.TRIP_FEE),
    AGENT_CALL_FEE(FeeKinds.AGENT_CALL_FEE);

    @JsonValue
    private final String value;

    FeeKind(String value) {
        this.value = value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static FeeKind fromValue(String value) {
        if (value != null) {
            for (FeeKind kind : values()) {
                if (kind.value.equals(value)) {
                    return kind;
                }
            }
        }
        throw new IllegalArgumentException("Unknown fee_kind: " + value);
    }
}
