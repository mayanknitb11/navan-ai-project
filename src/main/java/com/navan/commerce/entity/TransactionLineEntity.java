package com.navan.commerce.entity;

import com.navan.commerce.constant.DatabaseConstants;
import com.navan.commerce.domain.LineCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/**
 * One tax/fee/… line on a transaction — merged, category-discriminated (DB_SCHEMA §2 rule 2).
 *
 * Flat by design: no ORM association to the parent. {@code transactionId} is a plain FK column;
 * the DAO assembles parent+children with explicit key queries. Fields common to every category
 * stay real typed columns (name, amount); category-specific fields (the tax rate today) live in
 * {@code attributes} JSONB, written and read back by the owning line-category strategy.
 *
 * Write-once: PATCH replace-all deletes + reinserts rows, so instances are never mutated —
 * builder construction, no setters at all.
 */
@Entity
@Table(name = DatabaseConstants.TABLE_TRANSACTION_LINES)
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionLineEntity extends BaseEntity {

    @Column(name = "transaction_id", nullable = false, length = 40)
    private String transactionId;

    @Convert(converter = LineCategoryConverter.class)
    @Column(nullable = false, length = 16)
    private LineCategory category;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** Category-specific fields as JSON (tax: {"rate": …}); NULL when the category has none. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String attributes;
}
