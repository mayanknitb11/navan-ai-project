package com.navan.commerce.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * The only place money rounding lives (D6). All amounts are scale-2 {@code BigDecimal},
 * {@code HALF_EVEN}; no float/double ever touches a total.
 */
public final class MoneyUtils {

    public static final int SCALE = 2;

    private MoneyUtils() {
    }

    public static BigDecimal normalize(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal sum(Collection<BigDecimal> values) {
        return values.stream()
                .map(MoneyUtils::normalize)
                .reduce(BigDecimal.ZERO.setScale(SCALE, RoundingMode.UNNECESSARY), BigDecimal::add);
    }
}
