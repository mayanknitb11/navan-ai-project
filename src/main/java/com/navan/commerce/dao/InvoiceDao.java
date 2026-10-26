package com.navan.commerce.dao;

import com.navan.commerce.constant.DatabaseConstants;
import com.navan.commerce.entity.InvoiceEntity;
import com.navan.commerce.entity.InvoiceLineEntity;
import com.navan.commerce.repository.InvoiceLineRepository;
import com.navan.commerce.repository.InvoiceRepository;
import com.navan.commerce.util.OffsetPageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * All invoice data-access in one place. Entities are flat (no ORM associations): the snapshot is
 * persisted header → lines with explicit key references, and read back the same way. Each line
 * carries its own frozen tax/fee breakdown as JSONB, so there is no third level to fetch.
 * All reads return active (non-soft-deleted) invoices only.
 */
@Component
@RequiredArgsConstructor
public class InvoiceDao {

    /** Stable page order backed by idx_invoices_created_id. */
    private static final Sort PAGE_SORT = Sort.by(DatabaseConstants.SORT_CREATED_AT).ascending()
            .and(Sort.by(DatabaseConstants.SORT_ID).ascending());

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;

    /** Persists header + snapshot lines — one commit by the caller's transaction. */
    public InvoiceEntity createWithSnapshot(InvoiceEntity invoice, List<InvoiceLineEntity> lines) {
        invoiceRepository.save(invoice);
        invoiceLineRepository.saveAll(lines);
        return invoice;
    }

    /** Re-snapshot (invoice PATCH): bulk-remove the old snapshot lines, insert the new build. */
    public void replaceSnapshot(String invoiceId, List<InvoiceLineEntity> lines) {
        invoiceLineRepository.deleteByInvoiceId(invoiceId);
        invoiceLineRepository.saveAll(lines);
    }

    public Optional<InvoiceEntity> findById(String id) {
        return invoiceRepository.findByIdAndDeletedAtIsNull(id);
    }

    public List<InvoiceLineEntity> findLines(String invoiceId) {
        return invoiceLineRepository.findByInvoiceId(invoiceId);
    }

    /** One IN query for a whole page of invoices — no N+1. */
    public List<InvoiceLineEntity> findLinesByInvoiceIds(Collection<String> invoiceIds) {
        return invoiceIds.isEmpty() ? List.of() : invoiceLineRepository.findByInvoiceIdIn(invoiceIds);
    }

    public Page<InvoiceEntity> findPage(int limit, int offset) {
        return invoiceRepository.findAllByDeletedAtIsNull(new OffsetPageRequest(limit, offset, PAGE_SORT));
    }

    /**
     * Soft delete via plain JPA CRUD: stamp deleted_at and save() — one UPDATE with the @Version
     * check. Snapshot rows are retained with the invoice (logically dead); transactions untouched.
     */
    public void delete(InvoiceEntity invoice) {
        invoice.setDeletedAt(Instant.now());
        invoiceRepository.save(invoice);
    }
}
