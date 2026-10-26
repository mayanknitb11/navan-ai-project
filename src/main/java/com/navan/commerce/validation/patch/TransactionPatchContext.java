package com.navan.commerce.validation.patch;

import com.navan.commerce.domain.metadata.TransactionMetadata;
import com.navan.commerce.dto.PatchTransactionRequest;
import com.navan.commerce.entity.TransactionEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Carrier for the transaction PATCH validation chain. The entity is loaded by the service first
 * (404 precedes validation); handlers then guard and enrich. {@code validatedMetadata} is filled
 * by the metadata handler only when the request carries a metadata field.
 */
@Getter
public class TransactionPatchContext {

    private final PatchTransactionRequest request;
    private final TransactionEntity entity;

    @Setter
    private TransactionMetadata validatedMetadata;

    public TransactionPatchContext(PatchTransactionRequest request, TransactionEntity entity) {
        this.request = request;
        this.entity = entity;
    }
}
