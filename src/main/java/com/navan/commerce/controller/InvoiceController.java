package com.navan.commerce.controller;

import com.navan.commerce.constant.PaginationConstants;
import com.navan.commerce.dto.CreateInvoiceRequest;
import com.navan.commerce.dto.InvoiceResponse;
import com.navan.commerce.dto.PageResponse;
import com.navan.commerce.service.InvoiceService;
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
 * HTTP seam for invoices. Bodies for create and PATCH share one shape:
 * {@code {"transaction_ids": ["…", …]}} — a single-id invoice is a one-element list.
 */
@RestController
@RequestMapping("/invoices")
@Validated
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /** 201 with header + frozen line snapshots + tax/fee rollups + totals. */
    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable String id) {
        return invoiceService.get(id);
    }

    @GetMapping
    public PageResponse<InvoiceResponse> list(
            @RequestParam(defaultValue = PaginationConstants.DEFAULT_LIMIT)
            @Min(PaginationConstants.MIN_LIMIT) @Max(PaginationConstants.MAX_LIMIT) int limit,
            @RequestParam(defaultValue = PaginationConstants.DEFAULT_OFFSET)
            @Min(PaginationConstants.MIN_OFFSET) int offset) {
        return invoiceService.list(limit, offset);
    }

    /** Replaces the transaction id set; the snapshot is rebuilt atomically from current transactions. */
    @PatchMapping("/{id}")
    public InvoiceResponse patch(@PathVariable String id,
                                 @Valid @RequestBody CreateInvoiceRequest request) {
        return invoiceService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
