package com.navan.commerce.validation.invoice;

import com.navan.commerce.dto.CreateInvoiceRequest;
import com.navan.commerce.exception.ApiException;
import com.navan.commerce.exception.DuplicateTransactionIdsException;
import com.navan.commerce.exception.ErrorCode;
import com.navan.commerce.validation.ValidationHandler;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * Chain link 1 — id-list shape: one body shape only ({@code transaction_ids}; a single-id
 * invoice is a one-element list). Rejects empty/absent ids (400) and repeated ids (409, D4).
 */
@Component
public class InvoiceIdsValidationHandler implements ValidationHandler<InvoiceValidationContext> {

    @Override
    public void handle(InvoiceValidationContext context) {
        CreateInvoiceRequest request = context.getRequest();
        List<String> ids = request.getTransactionIds();
        if (ids == null || ids.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Provide a non-empty transaction_ids list");
        }
        if (ids.size() != new HashSet<>(ids).size()) {
            throw new DuplicateTransactionIdsException();
        }
        context.setTransactionIds(ids);
    }

    @Override
    public int order() {
        return 1;
    }
}
