package com.navan.commerce.entity;

import com.navan.commerce.constant.DatabaseConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Invoice aggregate root. Everything billed is physically copied into {@link InvoiceLineEntity}
 * snapshots, so later transaction edits cannot reach an issued invoice (spec §3). Soft delete
 * (D11): data stays in the DB; the DAO/repository layer filters {@code deletedAt IS NULL} on
 * every read, and delete is a plain JPA save() UPDATE. Deleting an invoice never touches the
 * underlying transactions, and the invoiced-guard only considers non-deleted invoices.
 * id / version / created_at / updated_at come from {@link BaseEntity}.
 *
 * Construction is via the builder. Setters exist ONLY on fields mutated by the PATCH re-snapshot
 * flow and the soft-delete stamp (managed-entity mutation — see TransactionEntity).
 */
@Entity
@Table(name = DatabaseConstants.TABLE_INVOICES)
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceEntity extends BaseEntity {

    @Setter // PATCH re-snapshot recomputes it
    @Column(nullable = false, length = 3)
    private String currency;

    @Setter // PATCH re-snapshot recomputes it
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Setter // PATCH re-snapshot recomputes it
    @Column(name = "line_count", nullable = false)
    private int lineCount;

    /** Soft-delete marker (D11): non-null = deleted. Filtered at the repository layer. */
    @Setter // lifecycle: soft delete stamps it on the managed entity
    @Column(name = "deleted_at")
    private Instant deletedAt;

    // No @OneToMany to invoice_lines by design: snapshot rows are fetched by the DAO via the
    // invoice_id FK — explicit queries instead of ORM relationship magic.
}
