package com.navan.commerce.mapper;

import com.navan.commerce.dto.LineItem;
import com.navan.commerce.entity.InvoiceLineEntity;
import com.navan.commerce.entity.TransactionEntity;
import com.navan.commerce.entity.TransactionLineEntity;
import com.navan.commerce.service.line.LineCategoryRegistry;
import com.navan.commerce.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * The invoice SNAPSHOT seam (spec §3) expressed as what it is — a mapper:
 * transaction entity → frozen invoice line row (deep copy). The metadata JSON string is
 * immutable, so copying the reference IS a deep copy of the blob; the money lines map 1:1 into
 * the line's {@code breakdown} as {@link LineItem}s — the SAME shape the transaction flow uses,
 * so a new line category or attribute flows to invoices with zero changes here.
 * After this mapping, edits to the source transaction physically cannot reach the invoice.
 */
@Component
@RequiredArgsConstructor
public class InvoiceSnapshotMapper {

    private final LineCategoryRegistry lineRegistry;

    /**
     * @param invoiceId            id of the invoice being built
     * @param transactionsInOrder  transactions in the client's requested id order
     * @param linesByTransactionId each transaction's current money lines, keyed by transaction id
     * @return frozen snapshot line rows (breakdown embedded), keyed to the invoice by FK
     */
    public List<InvoiceLineEntity> toSnapshotLines(String invoiceId,
                                                   List<TransactionEntity> transactionsInOrder,
                                                   Map<String, List<TransactionLineEntity>> linesByTransactionId) {
        return transactionsInOrder.stream()
                .map(source -> toSnapshotLine(invoiceId, source,
                        linesByTransactionId.getOrDefault(source.getId(), List.of())))
                .toList();
    }

    private InvoiceLineEntity toSnapshotLine(String invoiceId, TransactionEntity source,
                                             List<TransactionLineEntity> sourceLines) {
        return InvoiceLineEntity.builder()
                .id(IdGenerator.newId())
                .invoiceId(invoiceId)
                .transactionId(source.getId())
                .type(source.getType().getValue())
                .externalId(source.getExternalId())
                .occurredAt(source.getOccurredAt())
                .total(source.getTotal())
                .metadataSnapshot(source.getMetadataJson())
                .breakdown(toBreakdown(sourceLines))
                .build();
    }

    /** 1:1 copy into the canonical line shape — each strategy reads back its own attributes. */
    private List<LineItem> toBreakdown(List<TransactionLineEntity> sourceLines) {
        return sourceLines.stream()
                .map(l -> lineRegistry.of(l.getCategory()).toLineItem(l))
                .toList();
    }
}
