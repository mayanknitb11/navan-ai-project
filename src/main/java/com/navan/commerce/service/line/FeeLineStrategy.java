package com.navan.commerce.service.line;

import com.navan.commerce.domain.LineCategory;
import com.navan.commerce.dto.LineItem;
import com.navan.commerce.dto.CreateTransactionRequest;
import com.navan.commerce.dto.FeeLineEntry;
import com.navan.commerce.dto.PatchTransactionRequest;
import com.navan.commerce.dto.LineEntry;
import com.navan.commerce.entity.TransactionLineEntity;
import com.navan.commerce.util.IdGenerator;
import com.navan.commerce.util.MoneyUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything about FEE lines: owns the fee_lines wire list and its DTO; fees carry no
 * category-specific fields (a rate sent on the wire is rejected 400), so {@code attributes}
 * stays NULL; rollups group by name.
 */
@Component
public class FeeLineStrategy implements LineCategoryStrategy<FeeLineEntry> {

    @Override
    public LineCategory category() {
        return LineCategory.FEE;
    }

    @Override
    public List<FeeLineEntry> extract(CreateTransactionRequest request) {
        return request.getFeeLines();
    }

    @Override
    public List<FeeLineEntry> extract(PatchTransactionRequest request) {
        return request.getFeeLines();
    }

    @Override
    public TransactionLineEntity toEntity(FeeLineEntry wire, String transactionId) {
        return TransactionLineEntity.builder()
                .id(IdGenerator.newId())
                .transactionId(transactionId)
                .category(LineCategory.FEE)
                .name(wire.getName())
                .amount(MoneyUtils.normalize(wire.getAmount()))
                .build();
    }

    @Override
    public LineItem toLineItem(TransactionLineEntity line) {
        return LineItem.builder()
                .category(LineCategory.FEE)
                .name(line.getName())
                .amount(line.getAmount())
                .build();
    }

    @Override
    public LineEntry toResponseItem(LineItem item) {
        // rate deliberately NOT echoed for fees
        return FeeLineEntry.builder().name(item.getName()).amount(item.getAmount()).build();
    }

    @Override
    public List<LineEntry> rollup(List<LineItem> items) {
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        for (LineItem item : items) {
            if (item.getCategory() == LineCategory.FEE) {
                grouped.merge(item.getName(), item.getAmount(), BigDecimal::add);
            }
        }
        return grouped.entrySet().stream()
                .map(e -> toResponseItem(LineItem.builder()
                        .category(LineCategory.FEE)
                        .name(e.getKey())
                        .amount(MoneyUtils.normalize(e.getValue()))
                        .build()))
                .toList();
    }
}
