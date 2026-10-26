package com.navan.commerce.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Prefixed UUIDs (D7): self-describing, fit VARCHAR(40), no collisions. */
class IdGeneratorTest {

    @Test
    void transactionIdsArePrefixedAndFitColumn() {
        String id = IdGenerator.newTransactionId();
        assertThat(id).startsWith("txn_");
        assertThat(id).hasSize(4 + 36);
    }

    @Test
    void invoiceIdsArePrefixedAndFitColumn() {
        String id = IdGenerator.newInvoiceId();
        assertThat(id).startsWith("inv_");
        assertThat(id).hasSize(4 + 36);
    }

    @Test
    void idsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(IdGenerator.newTransactionId());
        }
        assertThat(ids).hasSize(10_000);
    }
}
