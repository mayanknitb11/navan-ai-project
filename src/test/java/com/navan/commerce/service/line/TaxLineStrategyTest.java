package com.navan.commerce.service.line;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.commerce.domain.LineCategory;
import com.navan.commerce.dto.LineEntry;
import com.navan.commerce.dto.LineItem;
import com.navan.commerce.dto.TaxLineEntry;
import com.navan.commerce.entity.TransactionLineEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Tax lines: optional rate persisted in attributes JSONB; rollup groups by (name, rate). */
class TaxLineStrategyTest {

    private final TaxLineStrategy strategy = new TaxLineStrategy(new ObjectMapper());

    @Test
    void rateRoundTripsThroughAttributesJson() {
        TaxLineEntry wire = TaxLineEntry.builder()
                .name("VAT").rate(new BigDecimal("0.07")).amount(new BigDecimal("21.70")).build();

        TransactionLineEntity entity = strategy.toEntity(wire, "txn_1");
        assertThat(entity.getCategory()).isEqualTo(LineCategory.TAX);
        assertThat(entity.getAttributes()).contains("rate");

        LineItem back = strategy.toLineItem(entity);
        assertThat(back.getName()).isEqualTo("VAT");
        assertThat(back.getRate()).isEqualByComparingTo("0.07");
        assertThat(back.getAmount()).isEqualByComparingTo("21.70");
    }

    @Test
    void absentRateLeavesAttributesNull() {
        TaxLineEntry wire = TaxLineEntry.builder()
                .name("Airport tax").amount(new BigDecimal("15.00")).build();

        TransactionLineEntity entity = strategy.toEntity(wire, "txn_1");
        assertThat(entity.getAttributes()).isNull();
        assertThat(strategy.toLineItem(entity).getRate()).isNull();
    }

    @Test
    void rollupGroupsByNameAndRate_nullRateIsItsOwnBucket() {
        List<LineItem> items = List.of(
                tax("VAT", "0.07", "21.70"),
                tax("VAT", "0.07", "10.00"),
                tax("VAT", null, "5.00"),       // same name, no rate → separate bucket
                tax("Airport tax", null, "15.00"),
                fee("YQ", "25.00"));            // must be ignored by the tax rollup

        List<LineEntry> rollup = strategy.rollup(items);

        assertThat(rollup).hasSize(3);
        assertThat(rollup).allSatisfy(e -> assertThat(e).isInstanceOf(TaxLineEntry.class));
        assertThat(find(rollup, "VAT", "0.07").getAmount()).isEqualByComparingTo("31.70");
        assertThat(find(rollup, "VAT", null).getAmount()).isEqualByComparingTo("5.00");
        assertThat(find(rollup, "Airport tax", null).getAmount()).isEqualByComparingTo("15.00");
    }

    private static LineEntry find(List<LineEntry> rollup, String name, String rate) {
        return rollup.stream()
                .map(TaxLineEntry.class::cast)
                .filter(e -> e.getName().equals(name)
                        && (rate == null ? e.getRate() == null : rate.equals(e.getRate().toPlainString())))
                .findFirst().orElseThrow();
    }

    private static LineItem tax(String name, String rate, String amount) {
        return LineItem.builder().category(LineCategory.TAX).name(name)
                .rate(rate == null ? null : new BigDecimal(rate)).amount(new BigDecimal(amount)).build();
    }

    private static LineItem fee(String name, String amount) {
        return LineItem.builder().category(LineCategory.FEE).name(name)
                .amount(new BigDecimal(amount)).build();
    }
}
