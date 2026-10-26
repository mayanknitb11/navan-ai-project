package com.navan.commerce.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.commerce.domain.LineCategory;
import com.navan.commerce.dto.LineItem;
import com.navan.commerce.dto.InvoiceLineResponse;
import com.navan.commerce.dto.InvoiceResponse;
import com.navan.commerce.dto.LineEntry;
import com.navan.commerce.entity.InvoiceEntity;
import com.navan.commerce.entity.InvoiceLineEntity;
import com.navan.commerce.service.line.LineCategoryRegistry;
import com.navan.commerce.service.line.LineCategoryStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Invoice assembly: header + flat snapshot rows → API response. Each line's frozen tax/fee split
 * arrives TYPED via the entity converter ({@code List<LineItem>}) and is split per category
 * strategy back into the API's separate tax_lines / fee_lines lists (spec L31).
 */
@Component
@RequiredArgsConstructor
public class InvoiceMapper {

    private final ObjectMapper objectMapper;
    private final LineCategoryRegistry lineRegistry;

    public InvoiceResponse toInvoiceResponse(InvoiceEntity invoice,
                                      List<InvoiceLineEntity> lines,
                                      List<LineEntry> taxRollup,
                                      List<LineEntry> feeRollup) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .currency(invoice.getCurrency())
                .total(invoice.getTotal())
                .lineCount(invoice.getLineCount())
                .lines(lines.stream()
                        .map(line -> toLineResponse(line, invoice.getCurrency()))
                        .toList())
                .taxRollup(taxRollup)
                .feeRollup(feeRollup)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    private InvoiceLineResponse toLineResponse(InvoiceLineEntity line, String currency) {
        List<LineItem> breakdown = line.getBreakdown();
        Map<LineCategory, List<LineEntry>> byCategory = lineRegistry.all().stream()
                .collect(Collectors.toMap(LineCategoryStrategy::category,
                        strategy -> breakdown.stream()
                                .filter(i -> i.getCategory() == strategy.category())
                                .map(strategy::toResponseItem)
                                .toList()));
        return InvoiceLineResponse.builder()
                .transactionId(line.getTransactionId())
                .type(line.getType())
                .externalId(line.getExternalId())
                // single currency per invoice (enforced at write); denormalized onto lines for clients
                .currency(currency)
                .occurredAt(line.getOccurredAt())
                .total(line.getTotal())
                // tax_lines / fee_lines are the spec's wire fields — the contract boundary
                .taxLines(byCategory.get(LineCategory.TAX))
                .feeLines(byCategory.get(LineCategory.FEE))
                .metadata(parseJsonTree(line.getMetadataSnapshot()))
                .build();
    }

    private JsonNode parseJsonTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored invoice metadata snapshot is unreadable", e);
        }
    }
}
