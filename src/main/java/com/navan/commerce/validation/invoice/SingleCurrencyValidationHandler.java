package com.navan.commerce.validation.invoice;

import com.navan.commerce.entity.TransactionEntity;
import com.navan.commerce.exception.MixedCurrencyException;
import com.navan.commerce.validation.ValidationHandler;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/** Chain link 3 — all transactions on one invoice must share a single currency (409, D-rules). */
@Component
public class SingleCurrencyValidationHandler implements ValidationHandler<InvoiceValidationContext> {

    @Override
    public void handle(InvoiceValidationContext context) {
        var currencies = context.getTransactions().stream()
                .map(TransactionEntity::getCurrency)
                .collect(Collectors.toSet());
        if (currencies.size() > 1) {
            throw new MixedCurrencyException(currencies);
        }
        context.setCurrency(context.getTransactions().get(0).getCurrency());
    }

    @Override
    public int order() {
        return 3;
    }
}
