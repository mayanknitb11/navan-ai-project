package com.navan.commerce.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.commerce.domain.LineCategory;
import com.navan.commerce.domain.TransactionType;
import com.navan.commerce.domain.metadata.TransactionMetadata;
import com.navan.commerce.dto.CreateTransactionRequest;
import com.navan.commerce.dto.LineEntry;
import com.navan.commerce.dto.TransactionResponse;
import com.navan.commerce.entity.TransactionEntity;
import com.navan.commerce.entity.TransactionLineEntity;
import com.navan.commerce.service.line.LineCategoryRegistry;
import com.navan.commerce.service.line.LineCategoryStrategy;
import com.navan.commerce.util.IdGenerator;
import com.navan.commerce.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transaction DTO ↔ entity translation — the only place metadata JSON is (de)serialized.
 * Entities are flat: the header is built here; money lines are converted by the per-category
 * strategies ({@link LineCategoryRegistry}) — the API contract keeps taxes and fees as SEPARATE
 * lists (spec L31), storage merges them with a category discriminator.
 */
@Component
@RequiredArgsConstructor
public class TransactionMapper {

    private final ObjectMapper objectMapper;
    private final LineCategoryRegistry lineRegistry;

    /** Header row only — lines are converted by the line strategies (see TransactionServiceImpl). */
    public TransactionEntity toTransactionEntity(CreateTransactionRequest request) {
        return TransactionEntity.builder()
                .id(IdGenerator.newTransactionId())
                .externalId(request.getExternalId())
                .type(request.getType())
                .currency(request.getCurrency())
                .occurredAt(request.getOccurredAt())
                .total(MoneyUtils.normalize(request.getTotal()))
                .metadataJson(serializeMetadata(request.getMetadata()))
                .build();
    }

    /** Assembles the response from a header row + its flat line rows (split per category strategy). */
    public TransactionResponse toTransactionResponse(TransactionEntity entity, List<TransactionLineEntity> lines) {
        Map<LineCategory, List<LineEntry>> byCategory = lineRegistry.all().stream()
                .collect(Collectors.toMap(LineCategoryStrategy::category,
                        strategy -> lines.stream()
                                .filter(l -> l.getCategory() == strategy.category())
                                .map(l -> strategy.toResponseItem(strategy.toLineItem(l)))
                                .toList()));
        return TransactionResponse.builder()
                .id(entity.getId())
                .type(entity.getType().getValue())
                .externalId(entity.getExternalId())
                .currency(entity.getCurrency())
                .occurredAt(entity.getOccurredAt())
                .total(entity.getTotal())
                // tax_lines / fee_lines are the spec's wire fields — the contract boundary
                .taxLines(byCategory.get(LineCategory.TAX))
                .feeLines(byCategory.get(LineCategory.FEE))
                .metadata(parseStoredMetadata(entity.getType(), entity.getMetadataJson()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public TransactionMetadata parseStoredMetadata(TransactionType type, String json) {
        try {
            return objectMapper.readValue(json, type.getMetadataClass());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored metadata is unreadable for type " + type.getValue(), e);
        }
    }

    public String serializeMetadata(TransactionMetadata metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Metadata could not be serialized", e);
        }
    }
}
