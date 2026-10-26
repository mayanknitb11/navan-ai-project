package com.navan.commerce.service;

import com.navan.commerce.dto.CreateInvoiceRequest;
import com.navan.commerce.dto.InvoiceResponse;
import com.navan.commerce.dto.PageResponse;

/** Invoice business operations. Invoices are frozen snapshots built from stored transactions. */
public interface InvoiceService {

    /**
     * Creates an invoice from one or more transaction ids. 404 if any id is missing,
     * 409 on mixed currencies or repeated ids; nothing is persisted in either case.
     */
    InvoiceResponse create(CreateInvoiceRequest request);

    /** 404 if missing (or soft-deleted). Rollups are computed from the frozen snapshot lines. */
    InvoiceResponse get(String id);

    /** Replaces the transaction id set and rebuilds the snapshot atomically from CURRENT transactions. */
    InvoiceResponse patch(String id, CreateInvoiceRequest request);

    /** Soft delete; underlying transactions are never touched. */
    void delete(String id);

    /** Bounded page — limit/offset are validated at the controller (1..100, >= 0). */
    PageResponse<InvoiceResponse> list(int limit, int offset);
}
