package com.navan.commerce.service.line;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.commerce.domain.LineCategory;
import com.navan.commerce.dto.LineItem;
import com.navan.commerce.dto.CreateTransactionRequest;
import com.navan.commerce.dto.PatchTransactionRequest;
import com.navan.commerce.dto.TaxLineEntry;
import com.navan.commerce.dto.LineEntry;
import com.navan.commerce.entity.TransactionLineEntity;
import com.navan.commerce.util.IdGenerator;
import com.navan.commerce.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything about TAX lines: owns the tax_lines wire list and its DTO; persists the optional
 * rate in {@code attributes} JSONB (only name/amount are columns); rollups group by (name, rate).
 */
@Component
@RequiredArgsConstructor
public class TaxLineStrategy implements LineCategoryStrategy<TaxLineEntry> {

    /** Category-specific fields persisted in {@code transaction_lines.attributes}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record TaxLineAttributes(BigDecimal rate) {
    }

    private record TaxKey(String name, BigDecimal rate) {
    }

    private final ObjectMapper objectMapper;

    @Override
    public LineCategory category() {
        return LineCategory.TAX;
    }

    @Override
    public List<TaxLineEntry> extract(CreateTransactionRequest request) {
        return request.getTaxLines();
    }

    @Override
    public List<TaxLineEntry> extract(PatchTransactionRequest request) {
        return request.getTaxLines();
    }

    @Override
    public TransactionLineEntity toEntity(TaxLineEntry wire, String transactionId) {
        return TransactionLineEntity.builder()
                .id(IdGenerator.newId())
                .transactionId(transactionId)
                .category(LineCategory.TAX)
                .name(wire.getName())
                .amount(MoneyUtils.normalize(wire.getAmount()))
                .attributes(serializeAttributes(wire.getRate())) // taxes carry the optional rate (spec)
                .build();
    }

    @Override
    public LineItem toLineItem(TransactionLineEntity line) {
        return LineItem.builder()
                .category(LineCategory.TAX)
                .name(line.getName())
                .rate(parseRate(line.getAttributes()))
                .amount(line.getAmount())
                .build();
    }

    @Override
    public LineEntry toResponseItem(LineItem item) {
        return TaxLineEntry.builder()
                .name(item.getName()).rate(item.getRate()).amount(item.getAmount()).build();
    }

    @Override
    public List<LineEntry> rollup(List<LineItem> items) {
        Map<TaxKey, BigDecimal> grouped = new LinkedHashMap<>();
        for (LineItem item : items) {
            if (item.getCategory() == LineCategory.TAX) {
                grouped.merge(new TaxKey(item.getName(), item.getRate()), item.getAmount(), BigDecimal::add);
            }
        }
        return grouped.entrySet().stream()
                .map(e -> toResponseItem(LineItem.builder()
                        .category(LineCategory.TAX)
                        .name(e.getKey().name())
                        .rate(e.getKey().rate())
                        .amount(MoneyUtils.normalize(e.getValue()))
                        .build()))
                .toList();
    }

    private String serializeAttributes(BigDecimal rate) {
        if (rate == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(new TaxLineAttributes(rate));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Tax line attributes could not be serialized", e);
        }
    }

    private BigDecimal parseRate(String attributes) {
        if (attributes == null) {
            return null;
        }
        try {
            return objectMapper.readValue(attributes, TaxLineAttributes.class).rate();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored tax line attributes are unreadable", e);
        }
    }
}
