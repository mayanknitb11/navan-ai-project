package com.navan.commerce.domain.metadata;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.navan.commerce.constant.TransactionTypes;

/**
 * Sealed root of the per-type metadata model (ARCHITECTURE §4, decision D2).
 *
 * Subtypes are bound by NAME; on {@code CreateTransactionRequest} the type id is read from the
 * sibling {@code type} property via {@code @JsonTypeInfo(include = EXTERNAL_PROPERTY)}. The stored
 * JSON column holds the plain subtype body (no type wrapper) — type is already a real column.
 *
 * Extensibility (OCP): a new transaction type = one {@link com.navan.commerce.domain.TransactionType}
 * value + one final class here + one {@code @JsonSubTypes} entry.
 */
@JsonSubTypes({
        @JsonSubTypes.Type(value = FlightMetadata.class, name = TransactionTypes.FLIGHT),
        @JsonSubTypes.Type(value = HotelMetadata.class, name = TransactionTypes.HOTEL),
        @JsonSubTypes.Type(value = RailMetadata.class, name = TransactionTypes.RAIL),
        @JsonSubTypes.Type(value = NavanFeeMetadata.class, name = TransactionTypes.NAVAN_FEE)
})
public sealed interface TransactionMetadata
        permits FlightMetadata, HotelMetadata, RailMetadata, NavanFeeMetadata {
}
