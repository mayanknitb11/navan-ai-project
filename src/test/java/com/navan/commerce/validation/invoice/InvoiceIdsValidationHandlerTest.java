package com.navan.commerce.validation.invoice;

import com.navan.commerce.dto.CreateInvoiceRequest;
import com.navan.commerce.exception.ApiException;
import com.navan.commerce.exception.DuplicateTransactionIdsException;
import com.navan.commerce.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Chain link 1: one body shape (transaction_ids); empty/absent → 400, repeated ids → 409 (D10). */
class InvoiceIdsValidationHandlerTest {

    private final InvoiceIdsValidationHandler handler = new InvoiceIdsValidationHandler();

    @Test
    void absentIdsFail400() {
        InvoiceValidationContext context = new InvoiceValidationContext(CreateInvoiceRequest.builder().build());

        assertThatThrownBy(() -> handler.handle(context))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void emptyIdsFail400() {
        InvoiceValidationContext context = new InvoiceValidationContext(
                CreateInvoiceRequest.builder().transactionIds(List.of()).build());

        assertThatThrownBy(() -> handler.handle(context))
                .isInstanceOfSatisfying(ApiException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void repeatedIdsFail409() {
        InvoiceValidationContext context = new InvoiceValidationContext(
                CreateInvoiceRequest.builder().transactionIds(List.of("txn_1", "txn_2", "txn_1")).build());

        assertThatThrownBy(() -> handler.handle(context))
                .isInstanceOfSatisfying(DuplicateTransactionIdsException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_TRANSACTION_IDS));
    }

    @Test
    void singleIdIsAOneElementList() {
        InvoiceValidationContext context = new InvoiceValidationContext(
                CreateInvoiceRequest.builder().transactionIds(List.of("txn_1")).build());

        handler.handle(context);

        assertThat(context.getTransactionIds()).containsExactly("txn_1");
    }

    @Test
    void distinctIdsPassThroughInOrder() {
        InvoiceValidationContext context = new InvoiceValidationContext(
                CreateInvoiceRequest.builder().transactionIds(List.of("txn_1", "txn_2", "txn_3")).build());

        handler.handle(context);

        assertThat(context.getTransactionIds()).containsExactly("txn_1", "txn_2", "txn_3");
    }
}
