package com.navan.commerce.validation.patch;

import com.navan.commerce.dao.TransactionDao;
import com.navan.commerce.exception.TransactionInvoicedException;
import com.navan.commerce.validation.ValidationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Chain link 1 — invoiced guard (spec §4): a transaction on any non-deleted invoice cannot be
 * modified. 409, persist nothing — the invoice snapshot stays frozen.
 */
@Component
@RequiredArgsConstructor
public class InvoicedTransactionValidationHandler implements ValidationHandler<TransactionPatchContext> {

    private final TransactionDao transactionDao;

    @Override
    public void handle(TransactionPatchContext context) {
        if (transactionDao.existsOnAnyActiveInvoice(context.getEntity().getId())) {
            throw new TransactionInvoicedException(context.getEntity().getId());
        }
    }

    @Override
    public int order() {
        return 1;
    }
}
