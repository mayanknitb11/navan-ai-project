package com.navan.commerce.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fee kinds (spec §1: at least trip_fee and agent_call_fee); unknown kinds must be rejected → 400. */
class FeeKindTest {

    @Test
    void parsesBothSpecKinds() {
        assertThat(FeeKind.fromValue("trip_fee")).isEqualTo(FeeKind.TRIP_FEE);
        assertThat(FeeKind.fromValue("agent_call_fee")).isEqualTo(FeeKind.AGENT_CALL_FEE);
    }

    @Test
    void rejectsNonCanonicalCasing() {
        assertThatThrownBy(() -> FeeKind.fromValue("TRIP_FEE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown fee_kind");
    }

    @Test
    void rejectsUnknownFeeKind() {
        assertThatThrownBy(() -> FeeKind.fromValue("pizza_fee"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown fee_kind");
    }
}
