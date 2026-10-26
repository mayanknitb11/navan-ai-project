package com.navan.commerce.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Strict canonical type parsing (D8); anything else must be rejected (spec: 400, no dummy metadata). */
class TransactionTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"flight", "hotel", "rail", "navan_fee"})
    void parsesCanonicalValues(String value) {
        assertThat(TransactionType.fromValue(value)).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"FLIGHT", "Flight", "Navan_fee", "NAVAN_FEE"})
    void rejectsNonCanonicalCasing(String value) {
        assertThatThrownBy(() -> TransactionType.fromValue(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown transaction type");
    }

    @Test
    void rejectsUnknownType() {
        assertThatThrownBy(() -> TransactionType.fromValue("spaceflight"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown transaction type");
    }

    @Test
    void rejectsNullType() {
        assertThatThrownBy(() -> TransactionType.fromValue(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eachTypeKnowsItsMetadataClass() {
        assertThat(TransactionType.FLIGHT.getMetadataClass().getSimpleName()).isEqualTo("FlightMetadata");
        assertThat(TransactionType.HOTEL.getMetadataClass().getSimpleName()).isEqualTo("HotelMetadata");
        assertThat(TransactionType.RAIL.getMetadataClass().getSimpleName()).isEqualTo("RailMetadata");
        assertThat(TransactionType.NAVAN_FEE.getMetadataClass().getSimpleName()).isEqualTo("NavanFeeMetadata");
    }
}
