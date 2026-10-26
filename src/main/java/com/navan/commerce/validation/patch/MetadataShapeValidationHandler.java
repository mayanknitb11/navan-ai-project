package com.navan.commerce.validation.patch;

import com.navan.commerce.validation.MetadataValidator;
import com.navan.commerce.validation.ValidationHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Chain link 2 — metadata shape: when the PATCH body carries metadata, parse it against the
 * entity's ACTUAL type and bean-validate (a flight cannot be patched into a hotel shape; 400).
 * The validated model is handed back via the context; the service owns applying it.
 */
@Component
@RequiredArgsConstructor
public class MetadataShapeValidationHandler implements ValidationHandler<TransactionPatchContext> {

    private final MetadataValidator metadataValidator;

    @Override
    public void handle(TransactionPatchContext context) {
        if (context.getRequest().getMetadata() != null) {
            context.setValidatedMetadata(metadataValidator.validateAndParse(
                    context.getRequest().getMetadata(), context.getEntity().getType()));
        }
    }

    @Override
    public int order() {
        return 2;
    }
}
