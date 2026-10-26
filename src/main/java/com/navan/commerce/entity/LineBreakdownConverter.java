package com.navan.commerce.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.navan.commerce.dto.LineItem;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * JPA converter: {@code invoice_lines.breakdown} JSONB ↔ {@code List<LineItem>} — same pattern
 * as the enum converters in this package (storage concerns stay in the entity layer).
 *
 * Uses a standalone ObjectMapper because JPA instantiates converters itself (no Spring DI);
 * configured to match the app's wire rules (snake_case, JavaTimeModule via findAndAddModules).
 */
@Converter
public class LineBreakdownConverter implements AttributeConverter<List<LineItem>, String> {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    private static final TypeReference<List<LineItem>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<LineItem> attribute) {
        try {
            return MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Line breakdown could not be serialized", e);
        }
    }

    @Override
    public List<LineItem> convertToEntityAttribute(String dbData) {
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored invoice line breakdown is unreadable", e);
        }
    }
}
