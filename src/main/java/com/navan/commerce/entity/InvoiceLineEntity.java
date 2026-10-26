package com.navan.commerce.entity;

import com.navan.commerce.constant.DatabaseConstants;
import com.navan.commerce.dto.LineItem;
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
import java.time.Instant;
import java.util.List;

/**
 * Frozen snapshot of one transaction on an invoice (physical copy — never a live join).
 * Flat by design: no ORM associations. {@code invoiceId} links up to the header;
 * {@code transactionId} is a plain reference (no DB FK, DB_SCHEMA rule 6) used only by the
 * invoiced-guard query on transaction PATCH/DELETE.
 *
 * The item's tax/fee split is the {@code breakdown} JSONB document (frozen, write-once,
 * read-whole — DB_SCHEMA §2 rule 4). Document schema: a list of {@link com.navan.commerce.dto.LineItem} via {@link LineBreakdownConverter}.
 *
 * Write-once (re-snapshot deletes + reinserts rows), so: builder construction, no setters.
 */
@Entity
@Table(name = DatabaseConstants.TABLE_INVOICE_LINES)
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineEntity extends BaseEntity {

    @Column(name = "invoice_id", nullable = false, length = 40)
    private String invoiceId;

    @Column(name = "transaction_id", nullable = false, length = 40)
    private String transactionId;

    @Column(nullable = false, length = 16)
    private String type;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_snapshot", nullable = false, columnDefinition = "jsonb")
    private String metadataSnapshot;

    /**
     * Frozen tax/fee split of this billed item — a TYPED list of {@link LineItem} (the same line
     * shape the transaction flow uses), persisted as JSONB via {@link LineBreakdownConverter}.
     * Adding a line category/attribute on the transaction side flows here automatically.
     */
    @Convert(converter = LineBreakdownConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "breakdown", nullable = false, columnDefinition = "jsonb")
    private List<LineItem> breakdown;
}
