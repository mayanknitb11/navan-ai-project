package com.navan.commerce.service.line;

import com.navan.commerce.domain.LineCategory;
import com.navan.commerce.dto.FeeLineEntry;
import com.navan.commerce.dto.LineEntry;
import com.navan.commerce.dto.LineItem;
import com.navan.commerce.entity.TransactionLineEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Fee lines: no category-specific attributes; rollup groups by name only. */
class FeeLineStrategyTest {

    private final FeeLineStrategy strategy = new FeeLineStrategy();

    @Test
    void feeEntityCarriesNoAttributes() {
        FeeLineEntry wire = FeeLineEntry.builder()
                .name("YQ").amount(new BigDecimal("25.00")).build();

        TransactionLineEntity entity = strategy.toEntity(wire, "txn_1");
        assertThat(entity.getCategory()).isEqualTo(LineCategory.FEE);
        assertThat(entity.getAttributes()).isNull();

        LineItem back = strategy.toLineItem(entity);
        assertThat(back.getName()).isEqualTo("YQ");
        assertThat(back.getAmount()).isEqualByComparingTo("25.00");
        assertThat(back.getRate()).isNull();
    }

    @Test
    void rollupGroupsByNameAndIgnoresTaxItems() {
        List<LineItem> items = List.of(
                fee("Booking fee", "4.00"),
                fee("Booking fee", "6.00"),
                fee("YQ", "25.00"),
                tax("VAT", "21.70")); // must be ignored by the fee rollup

        List<LineEntry> rollup = strategy.rollup(items);

        assertThat(rollup).hasSize(2);
        assertThat(rollup).allSatisfy(e -> assertThat(e).isInstanceOf(FeeLineEntry.class));
        assertThat(find(rollup, "Booking fee").getAmount()).isEqualByComparingTo("10.00");
        assertThat(find(rollup, "YQ").getAmount()).isEqualByComparingTo("25.00");
    }

    private static LineEntry find(List<LineEntry> rollup, String name) {
        return rollup.stream().filter(e -> e.getName().equals(name)).findFirst().orElseThrow();
    }

    private static LineItem fee(String name, String amount) {
        return LineItem.builder().category(LineCategory.FEE).name(name)
                .amount(new BigDecimal(amount)).build();
    }

    private static LineItem tax(String name, String amount) {
        return LineItem.builder().category(LineCategory.TAX).name(name)
                .amount(new BigDecimal(amount)).build();
    }
}
