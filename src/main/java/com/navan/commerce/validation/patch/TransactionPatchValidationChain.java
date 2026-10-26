package com.navan.commerce.validation.patch;

import com.navan.commerce.dto.PatchTransactionRequest;
import com.navan.commerce.entity.TransactionEntity;
import com.navan.commerce.validation.ValidationHandler;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * The transaction PATCH validation flow as a Chain of Responsibility:
 * invoiced-guard (409) → metadata shape (400). New PATCH rule = one new handler class.
 */
@Component
public class TransactionPatchValidationChain {

    private final List<ValidationHandler<TransactionPatchContext>> handlers;

    public TransactionPatchValidationChain(List<ValidationHandler<TransactionPatchContext>> handlers) {
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(ValidationHandler::order))
                .toList();
    }

    /** Runs the chain; returns the enriched context (validated metadata when present). */
    public TransactionPatchContext validate(PatchTransactionRequest request, TransactionEntity entity) {
        TransactionPatchContext context = new TransactionPatchContext(request, entity);
        handlers.forEach(handler -> handler.handle(context));
        return context;
    }
}
