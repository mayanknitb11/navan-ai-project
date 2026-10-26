package com.navan.commerce.entity;

import com.navan.commerce.domain.TransactionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores the canonical lowercase wire value ("navan_fee") rather than the enum constant name
 * ("NAVAN_FEE") so the DB matches DB_SCHEMA.md and the chk_transactions_type CHECK constraint.
 */
@Converter
public class TransactionTypeConverter implements AttributeConverter<TransactionType, String> {

    @Override
    public String convertToDatabaseColumn(TransactionType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TransactionType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TransactionType.fromValue(dbData);
    }
}
