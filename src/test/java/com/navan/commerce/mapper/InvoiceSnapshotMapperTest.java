package com.navan.commerce.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.commerce.domain.LineCategory;
import com.navan.commerce.domain.TransactionType;
import com.navan.commerce.dto.LineItem;
import com.navan.commerce.entity.InvoiceLineEntity;
import com.navan.commerce.entity.TransactionEntity;
import com.navan.commerce.entity.TransactionLineEntity;
import com.navan.commerce.service.line.FeeLineStrategy;
import com.navan.commerce.service.line.LineCategoryRegistry;
import com.navan.commerce.service.line.TaxLineStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The snapshot seam (spec §3): invoice lines are DEEP COPIES of the transaction at invoice
 * time — later edits to the source must not change the snapshot.
 */
class InvoiceSnapshotMapperTest {

    private final InvoiceSnapshotMapper mapper = new InvoiceSnapshotMapper(
            new LineCategoryRegistry(List.of(new TaxLineStrategy(new ObjectMapper()), new FeeLineStrategy())));

    @Test
    void snapshotCopiesEveryFieldAndBothLineCategories() {
        TransactionEntity source = TransactionEntity.builder()
                .id("txn_1").externalId("ext-1").type(TransactionType.FLIGHT)
                .currency("EUR").occurredAt(Instant.parse("2026-03-11T06:40:00Z"))
                .total(new BigDecimal("220.00")).metadataJson("{\"origin\":\"TXL\"}").build();
        List<TransactionLineEntity> sourceLines = List.of(
                line(LineCategory.TAX, "Airport tax", "15.00", null),
                line(LineCategory.FEE, "YQ", "25.00", null));

        List<InvoiceLineEntity> lines = mapper.toSnapshotLines("inv_1", List.of(source),
                Map.of("txn_1", sourceLines));

        assertThat(lines).hasSize(1);
        InvoiceLineEntity line = lines.get(0);
        assertThat(line.getInvoiceId()).isEqualTo("inv_1");
        assertThat(line.getTransactionId()).isEqualTo("txn_1");
        assertThat(line.getType()).isEqualTo("flight");
        assertThat(line.getExternalId()).isEqualTo("ext-1");
        assertThat(line.getOccurredAt()).isEqualTo(Instant.parse("2026-03-11T06:40:00Z"));
        assertThat(line.getTotal()).isEqualByComparingTo("220.00");
        assertThat(line.getMetadataSnapshot()).isEqualTo("{\"origin\":\"TXL\"}");
        assertThat(line.getBreakdown()).extracting(LineItem::getName)
                .containsExactlyInAnyOrder("Airport tax", "YQ");
    }

    @Test
    void snapshotIsIndependentOfLaterSourceMutation() {
        TransactionEntity source = TransactionEntity.builder()
                .id("txn_1").externalId("ext-1").type(TransactionType.FLIGHT)
                .currency("EUR").occurredAt(Instant.parse("2026-03-11T06:40:00Z"))
                .total(new BigDecimal("220.00")).metadataJson("{\"origin\":\"TXL\"}").build();

        InvoiceLineEntity snapshot = mapper.toSnapshotLines("inv_1", List.of(source),
                Map.of("txn_1", List.of(line(LineCategory.TAX, "VAT", "21.70", "{\"rate\":0.07}"))))
                .get(0);

        // Mutate the source AFTER snapshotting (the freeze rule, spec test #4)
        source.setTotal(new BigDecimal("999.00"));
        source.setMetadataJson("{\"origin\":\"BOM\"}");

        assertThat(snapshot.getTotal()).isEqualByComparingTo("220.00");
        assertThat(snapshot.getMetadataSnapshot()).isEqualTo("{\"origin\":\"TXL\"}");
        // breakdown carries typed values read back through the owning strategy
        LineItem vat = snapshot.getBreakdown().get(0);
        assertThat(vat.getCategory()).isEqualTo(LineCategory.TAX);
        assertThat(vat.getRate()).isEqualByComparingTo("0.07");
        assertThat(vat.getAmount()).isEqualByComparingTo("21.70");
    }

    @Test
    void missingSourceLinesYieldEmptyBreakdown() {
        TransactionEntity source = TransactionEntity.builder()
                .id("txn_1").externalId("ext-1").type(TransactionType.NAVAN_FEE)
                .currency("EUR").occurredAt(Instant.parse("2026-03-11T06:40:00Z"))
                .total(new BigDecimal("25.00")).metadataJson("{\"fee_kind\":\"trip_fee\"}").build();

        InvoiceLineEntity snapshot = mapper.toSnapshotLines("inv_1", List.of(source), Map.of()).get(0);

        assertThat(snapshot.getBreakdown()).isEmpty();
    }

    private static TransactionLineEntity line(LineCategory category, String name, String amount, String attributes) {
        return TransactionLineEntity.builder()
                .id("line-1").transactionId("txn_1").category(category)
                .name(name).amount(new BigDecimal(amount)).attributes(attributes).build();
    }
}
