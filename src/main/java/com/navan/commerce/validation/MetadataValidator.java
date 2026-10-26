package com.navan.commerce.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.commerce.domain.TransactionType;
import com.navan.commerce.domain.metadata.TransactionMetadata;
import com.navan.commerce.exception.ApiException;
import com.navan.commerce.exception.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates PATCHed metadata (spec §4). Two gates, in order:
 * 1. SHAPE — the raw JSON tree is parsed against the transaction's ACTUAL type, so a flight can
 *    never be patched into a hotel shape (parse failure → 400).
 * 2. CONSTRAINTS — bean validation on the typed model (e.g. rail legs must be non-empty),
 *    run programmatically because the PATCH body carries metadata as an untyped JsonNode.
 */
@Component
@RequiredArgsConstructor
public class MetadataValidator {

    private final Validator validator;
    private final ObjectMapper objectMapper;

    /** Returns the validated, typed metadata — or throws ApiException(400) with field detail. */
    public TransactionMetadata validateAndParse(JsonNode metadataNode, TransactionType actualType) {
        TransactionMetadata metadata = parseAgainstActualType(metadataNode, actualType);
        Set<ConstraintViolation<TransactionMetadata>> violations = validator.validate(metadata);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Invalid metadata: " + detail);
        }
        return metadata;
    }

    private TransactionMetadata parseAgainstActualType(JsonNode node, TransactionType actualType) {
        try {
            return objectMapper.treeToValue(node, actualType.getMetadataClass());
        } catch (JsonProcessingException e) {
            // Wrong shape for the stored type (e.g. hotel fields onto a rail transaction) → 400,
            // never a 500: this is a client error, not a server fault.
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "metadata does not match type '" + actualType.getValue() + "'");
        }
    }
}
