package com.navan.commerce.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.navan.commerce.constant.LineCategories;
import lombok.Getter;

/**
 * Discriminator for the merged lines tables. The wire contract keeps taxes and fees as SEPARATE
 * lists (spec L31) — this enum exists at the storage layer only; mappers split/merge by it.
 */
@Getter
public enum LineCategory {

    TAX(LineCategories.TAX),
    FEE(LineCategories.FEE);

    @JsonValue
    private final String value;

    LineCategory(String value) {
        this.value = value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static LineCategory fromValue(String value) {
        if (value != null) {
            for (LineCategory category : values()) {
                if (category.value.equalsIgnoreCase(value.strip())) {
                    return category;
                }
            }
        }
        throw new IllegalArgumentException("Unknown line category: " + value);
    }
}
