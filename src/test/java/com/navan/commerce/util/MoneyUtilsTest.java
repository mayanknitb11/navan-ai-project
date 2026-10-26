package com.navan.commerce.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Money rules (D6): scale-2 BigDecimal, HALF_EVEN — never IEEE float arithmetic. */
class MoneyUtilsTest {

    @Test
    void normalizeSetsScale2() {
        assertThat(MoneyUtils.normalize(new BigDecimal("0.1")).toPlainString()).isEqualTo("0.10");
        assertThat(MoneyUtils.normalize(new BigDecimal("220")).toPlainString()).isEqualTo("220.00");
    }

    @Test
    void normalizeRoundsHalfEven() {
        assertThat(MoneyUtils.normalize(new BigDecimal("10.005"))).isEqualByComparingTo("10.00"); // 0 is even
        assertThat(MoneyUtils.normalize(new BigDecimal("10.015"))).isEqualByComparingTo("10.02"); // 1 is odd
    }

    @Test
    void sumMatchesFixtureGrandTotal() {
        BigDecimal total = MoneyUtils.sum(List.of(
                new BigDecimal("220.00"), new BigDecimal("331.70"), new BigDecimal("94.00"),
                new BigDecimal("25.00"), new BigDecimal("15.00")));
        assertThat(total).isEqualByComparingTo("685.70");
        assertThat(total.scale()).isEqualTo(2);
    }

    @Test
    void sumOfEmptyIsZero() {
        assertThat(MoneyUtils.sum(List.of())).isEqualByComparingTo("0.00");
    }

    @Test
    void sumAvoidsFloatArtifacts() {
        // 0.1 + 0.2 in double is 0.30000000000000004 — BigDecimal must not be
        BigDecimal sum = MoneyUtils.sum(List.of(new BigDecimal("0.10"), new BigDecimal("0.20")));
        assertThat(sum.toPlainString()).isEqualTo("0.30");
    }
}
