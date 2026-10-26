package com.navan.commerce.service;

import com.navan.commerce.dto.CreateTransactionRequest;
import com.navan.commerce.dto.PatchTransactionRequest;
import com.navan.commerce.dto.PageResponse;
import com.navan.commerce.dto.TransactionResponse;

/** Transaction business operations. Implementations enforce the duplicate/invoiced/money rules. */
public interface TransactionService {

    /** Creates a transaction atomically (header + tax lines + fee lines + metadata). 409 on duplicate external_id. */
    TransactionResponse create(CreateTransactionRequest request);

    /** 404 if missing (or soft-deleted). */
    TransactionResponse get(String id);

    /** Partial update. 404 if missing; 409 if the transaction is on any non-deleted invoice. */
    TransactionResponse patch(String id, PatchTransactionRequest request);

    /**
     * Soft delete. 404 if missing. {@code force=false} (default): 409 if on any non-deleted
     * invoice. {@code force=true}: skips the invoiced-guard — the invoice is unaffected
     * either way because it holds a frozen snapshot, not a live reference.
     */
    void delete(String id, boolean force);

    /** Bounded page — limit/offset are validated at the controller (1..100, >= 0). */
    PageResponse<TransactionResponse> list(int limit, int offset);
}
