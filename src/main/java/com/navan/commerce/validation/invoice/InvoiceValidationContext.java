package com.navan.commerce.validation.invoice;

import com.navan.commerce.dto.CreateInvoiceRequest;
import com.navan.commerce.entity.TransactionEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Carrier for the invoice validation chain: starts from the request; each handler fills the next
 * stage (ids → loaded transactions → currency). Setters exist because a chain context is
 * progressively enriched by handlers — that mutation is its whole job.
 */
@Getter
public class InvoiceValidationContext {

    private final CreateInvoiceRequest request;

    @Setter
    private List<String> transactionIds;

    @Setter
    private List<TransactionEntity> transactions;

    @Setter
    private String currency;

    public InvoiceValidationContext(CreateInvoiceRequest request) {
        this.request = request;
    }
}
