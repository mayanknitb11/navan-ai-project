package com.navan.commerce.controller;

import com.navan.commerce.constant.PaginationConstants;
import com.navan.commerce.dto.CreateTransactionRequest;
import com.navan.commerce.dto.PatchTransactionRequest;
import com.navan.commerce.dto.PageResponse;
import com.navan.commerce.dto.TransactionResponse;
import com.navan.commerce.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP seam for transactions — routing and status codes only; all rules live in the transactionService.
 * Pagination: limit defaults to 50, must be 1..100 (limit=1000 → 400, decision D5); offset >= 0.
 */
@RestController
@RequestMapping("/transactions")
@Validated
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /** 201 with the stored record; duplicate external_id → 409 with the existing id (D3). */
    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.create(request));
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable String id) {
        return transactionService.get(id);
    }

    @GetMapping
    public PageResponse<TransactionResponse> list(
            @RequestParam(defaultValue = PaginationConstants.DEFAULT_LIMIT)
            @Min(PaginationConstants.MIN_LIMIT) @Max(PaginationConstants.MAX_LIMIT) int limit,
            @RequestParam(defaultValue = PaginationConstants.DEFAULT_OFFSET)
            @Min(PaginationConstants.MIN_OFFSET) int offset) {
        return transactionService.list(limit, offset);
    }

    @PatchMapping("/{id}")
    public TransactionResponse patch(@PathVariable String id,
                                     @Valid @RequestBody PatchTransactionRequest request) {
        return transactionService.patch(id, request);
    }

    /**
     * Soft delete. {@code force=false} (default): 409 if the transaction is on any invoice.
     * {@code force=true}: skips the invoiced-guard — safe because invoices are frozen
     * snapshots (physical copies, no FK back); invoice data is never impacted.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @RequestParam(defaultValue = "false") boolean force) {
        transactionService.delete(id, force);
        return ResponseEntity.noContent().build();
    }
}
