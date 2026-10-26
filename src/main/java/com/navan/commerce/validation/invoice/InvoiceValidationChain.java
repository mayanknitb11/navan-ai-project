package com.navan.commerce.validation.invoice;

import com.navan.commerce.dto.CreateInvoiceRequest;
import com.navan.commerce.validation.ValidationHandler;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * The invoice create/PATCH validation flow as a Chain of Responsibility:
 * id-list shape (400/409) → existence (404) → single currency (409). Spring injects every
 * handler bean; the chain sorts by {@code order()} so a new rule = one new handler class,
 * no caller changes.
 */
@Component
public class InvoiceValidationChain {

    private final List<ValidationHandler<InvoiceValidationContext>> handlers;

    public InvoiceValidationChain(List<ValidationHandler<InvoiceValidationContext>> handlers) {
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(ValidationHandler::order))
                .toList();
    }

    /** Runs the chain; returns the fully enriched context (ids, transactions, currency). */
    public InvoiceValidationContext validate(CreateInvoiceRequest request) {
        InvoiceValidationContext context = new InvoiceValidationContext(request);
        handlers.forEach(handler -> handler.handle(context));
        return context;
    }
}
