package com.navan.commerce.entity;

import com.navan.commerce.domain.LineCategory;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores the canonical lowercase value ("tax") rather than the enum constant name ("TAX"). */
@Converter
public class LineCategoryConverter implements AttributeConverter<LineCategory, String> {

    @Override
    public String convertToDatabaseColumn(LineCategory attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public LineCategory convertToEntityAttribute(String dbData) {
        return dbData == null ? null : LineCategory.fromValue(dbData);
    }
}
