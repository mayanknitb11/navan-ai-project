package com.navan.commerce.validation.invoice;

import com.navan.commerce.dao.TransactionDao;
import com.navan.commerce.entity.TransactionEntity;
import com.navan.commerce.exception.ErrorCode;
import com.navan.commerce.exception.NotFoundException;
import com.navan.commerce.validation.ValidationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Chain link 2 — existence: loads all referenced transactions (one IN query); ANY missing id →
 * 404 and nothing is persisted. Order follows the client's request.
 */
@Component
@RequiredArgsConstructor
public class TransactionExistenceValidationHandler implements ValidationHandler<InvoiceValidationContext> {

    private final TransactionDao transactionDao;

    @Override
    public void handle(InvoiceValidationContext context) {
        Map<String, TransactionEntity> byId = transactionDao.findAllByIds(context.getTransactionIds())
                .stream()
                .collect(Collectors.toMap(TransactionEntity::getId, Function.identity()));
        List<TransactionEntity> transactions = context.getTransactionIds().stream()
                .map(id -> Optional.ofNullable(byId.get(id))
                        .orElseThrow(() -> new NotFoundException(ErrorCode.TRANSACTION_NOT_FOUND, id)))
                .toList();
        context.setTransactions(transactions);
    }

    @Override
    public int order() {
        return 2;
    }
}
