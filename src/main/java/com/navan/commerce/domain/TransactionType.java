package com.navan.commerce.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.navan.commerce.constant.TransactionTypes;
import com.navan.commerce.domain.metadata.FlightMetadata;
import com.navan.commerce.domain.metadata.NavanFeeMetadata;
import com.navan.commerce.domain.metadata.HotelMetadata;
import com.navan.commerce.domain.metadata.RailMetadata;
import com.navan.commerce.domain.metadata.TransactionMetadata;
import lombok.Getter;

/**
 * The four transaction types (spec §1). {@code value} is the canonical lowercase wire/DB form
 * (D8) — exact match only; anything else surfaces as 400 UNKNOWN_TYPE. Each type knows its
 * metadata subtype, which is how the mapper deserializes the stored JSON blob back into a
 * typed model.
 */
@Getter
public enum TransactionType {

    FLIGHT(TransactionTypes.FLIGHT, FlightMetadata.class),
    HOTEL(TransactionTypes.HOTEL, HotelMetadata.class),
    RAIL(TransactionTypes.RAIL, RailMetadata.class),
    NAVAN_FEE(TransactionTypes.NAVAN_FEE, NavanFeeMetadata.class);

    @JsonValue
    private final String value;
    private final Class<? extends TransactionMetadata> metadataClass;

    TransactionType(String value, Class<? extends TransactionMetadata> metadataClass) {
        this.value = value;
        this.metadataClass = metadataClass;
    }

    /** Exact canonical match; unknown values surface as 400 UNKNOWN_TYPE via the exception handler. */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TransactionType fromValue(String value) {
        if (value != null) {
            for (TransactionType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unknown transaction type: " + value);
    }
}
