package com.navan.commerce.repository;

import com.navan.commerce.entity.InvoiceLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLineEntity, String> {

    /**
     * Invoiced-guard: is this transaction on any NON-DELETED invoice? Native SQL on purpose —
     * it must match DB_SCHEMA §5 exactly. Resolved by idx_invoice_lines_txn + the invoices PK:
     * O(log n), no scan.
     */
    @Query(value = """
            SELECT EXISTS(
                SELECT 1
                FROM invoice_lines il
                JOIN invoices i ON i.id = il.invoice_id
                WHERE il.transaction_id = :transactionId
                  AND i.deleted_at IS NULL
            )
            """, nativeQuery = true)
    boolean existsOnAnyActiveInvoice(@Param("transactionId") String transactionId);

    List<InvoiceLineEntity> findByInvoiceId(String invoiceId);

    /** One IN query for a whole page of invoices — no N+1. */
    List<InvoiceLineEntity> findByInvoiceIdIn(Collection<String> invoiceIds);

    /** Re-snapshot support: bulk-remove the invoice's previous lines. */
    void deleteByInvoiceId(String invoiceId);
}
