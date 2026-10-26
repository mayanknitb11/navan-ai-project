package com.navan.commerce.entity;

import com.navan.commerce.constant.DatabaseConstants;
import com.navan.commerce.domain.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transaction aggregate root (table: transactions).
 *
 * - Soft delete (D11): data stays in the DB; {@code deletedAt} marks the row dead. No Hibernate
 *   magic — the DAO/repository layer filters with {@code deletedAt IS NULL} on every read, and
 *   delete is a plain JPA save() UPDATE (which also bumps @Version, so a concurrent PATCH/DELETE
 *   loser still gets 409). The column is never exposed in any API response.
 * - {@code metadata} is the type-specific JSON blob stored as a plain String; the mapper converts
 *   it to/from the sealed model using {@link TransactionType#getMetadataClass()}.
 * - id / version / created_at / updated_at come from {@link BaseEntity}.
 *
 * Construction is via the builder. Setters exist ONLY on fields that legitimately mutate after
 * creation: the PATCH-able fields and the soft-delete stamp (JPA dirty-checking mutates the
 * managed instance — rebuilding it would fight the persistence context). Identity fields
 * (externalId, type) are immutable by construction (D-rule: external_id is the duplicate-guard
 * identity; type changing would orphan the metadata subtype).
 */
@Entity
@Table(name = DatabaseConstants.TABLE_TRANSACTIONS)
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity extends BaseEntity {

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Convert(converter = TransactionTypeConverter.class)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @Setter // PATCH-able
    @Column(nullable = false, length = 3)
    private String currency;

    @Setter // PATCH-able
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Setter // PATCH-able
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Setter // PATCH-able
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private String metadataJson;

    /** Soft-delete marker (D11): non-null = deleted. Filtered at the repository layer. */
    @Setter // lifecycle: soft delete stamps it on the managed entity
    @Column(name = "deleted_at")
    private Instant deletedAt;

    // No @OneToMany to transaction_lines by design: child rows are fetched by the DAO via the
    // transaction_id FK — explicit queries instead of ORM relationship magic.
}
