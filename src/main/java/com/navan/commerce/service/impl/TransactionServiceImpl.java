package com.navan.commerce.service.impl;

import com.navan.commerce.dao.TransactionDao;
import com.navan.commerce.dto.CreateTransactionRequest;
import com.navan.commerce.dto.PatchTransactionRequest;
import com.navan.commerce.dto.PageResponse;
import com.navan.commerce.dto.TransactionResponse;
import com.navan.commerce.entity.TransactionEntity;
import com.navan.commerce.entity.TransactionLineEntity;
import com.navan.commerce.exception.DuplicateTransactionException;
import com.navan.commerce.exception.ErrorCode;
import com.navan.commerce.exception.NotFoundException;
import com.navan.commerce.exception.TransactionInvoicedException;
import com.navan.commerce.mapper.TransactionMapper;
import com.navan.commerce.service.TransactionService;
import com.navan.commerce.service.line.LineCategoryRegistry;
import com.navan.commerce.service.line.LineCategoryStrategy;
import com.navan.commerce.util.MoneyUtils;
import com.navan.commerce.validation.patch.InvoicedTransactionValidationHandler;
import com.navan.commerce.validation.patch.TransactionPatchContext;
import com.navan.commerce.validation.patch.TransactionPatchValidationChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Isolation: the default READ_COMMITTED is sufficient everywhere here — the duplicate guard is
 * the partial unique index (it serializes conflicting inserts at the index entry under ANY
 * isolation level), and PATCH/DELETE atomicity comes from @Version optimistic locking. Declaring
 * SERIALIZABLE would only add serialization-failure retries without buying correctness.
 *
 * Entities are flat: this service assembles header + line rows via the DAO's key queries.
 * PATCH/DELETE rules live in the validation chain (invoiced-guard, metadata shape).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionDao transactionDao;
    private final TransactionMapper transactionMapper;
    private final LineCategoryRegistry lineCategoryRegistry;
    private final TransactionPatchValidationChain patchValidationChain;
    private final InvoicedTransactionValidationHandler invoicedGuardHandler;

    /**
     * No check-then-act: the partial unique index on external_id is the ONLY duplicate guard.
     * The DAO flushes the header inside the try so the constraint fires HERE, before any child
     * insert; the loser of a concurrent race then reads the winner's id in a fresh transaction
     * (the current one is aborted by Postgres).
     */
    @Override
    @Transactional
    public TransactionResponse create(CreateTransactionRequest request) {
        TransactionEntity entity = transactionMapper.toTransactionEntity(request);
        // Every registered line category contributes its lines — a new category needs no change here.
        List<TransactionLineEntity> lines = lineCategoryRegistry.all().stream()
                .flatMap(strategy -> strategy.toEntities(request, entity.getId()).stream())
                .toList();
        try {
            transactionDao.createWithLines(entity, lines);
        } catch (DataIntegrityViolationException e) {
            String existingId = transactionDao
                    .findActiveIdByExternalIdInNewTransaction(request.getExternalId())
                    .orElseThrow(() -> e);
            log.info("Duplicate create rejected: external_id={} already exists as {}", request.getExternalId(), existingId);
            throw new DuplicateTransactionException(existingId);
        }
        log.info("Created transaction id={} type={} external_id={}",
                entity.getId(), entity.getType().getValue(), entity.getExternalId());
        return transactionMapper.toTransactionResponse(entity, lines);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse get(String id) {
        TransactionEntity entity = findActiveByIdOrThrow(id);
        return transactionMapper.toTransactionResponse(entity, transactionDao.findLines(id));
    }

    /**
     * PATCH: 404 first, then the validation chain (invoiced-guard 409, metadata shape 400).
     * Absent fields are left untouched; tax_lines / fee_lines use per-category replace-all
     * inside the same transaction.
     */
    @Override
    @Transactional
    public TransactionResponse patch(String id, PatchTransactionRequest request) {
        TransactionEntity entity = findActiveByIdOrThrow(id);
        TransactionPatchContext context = patchValidationChain.validate(request, entity);

        if (request.getCurrency() != null) {
            entity.setCurrency(request.getCurrency());
        }
        if (request.getOccurredAt() != null) {
            entity.setOccurredAt(request.getOccurredAt());
        }
        if (request.getTotal() != null) {
            entity.setTotal(MoneyUtils.normalize(request.getTotal()));
        }
        if (context.getValidatedMetadata() != null) {
            entity.setMetadataJson(transactionMapper.serializeMetadata(context.getValidatedMetadata()));
        }
        // Per-category replace-all, uniformly: absent wire list = leave stored rows untouched.
        for (LineCategoryStrategy<?> strategy : lineCategoryRegistry.all()) {
            List<TransactionLineEntity> wireLines = strategy.toEntities(request, id);
            if (wireLines != null) {
                transactionDao.replaceLines(id, strategy.category(), wireLines);
            }
        }
        // Header dirty-checks at commit; @Version rejects a concurrent loser with 409.
        log.info("Patched transaction id={}", id);
        return transactionMapper.toTransactionResponse(entity, transactionDao.findLines(id));
    }

    @Override
    @Transactional
    public void delete(String id, boolean force) {
        TransactionEntity entity = findActiveByIdOrThrow(id);
        // Same guard rule as PATCH (one definition, reused from the patch chain) — skipped when
        // the caller explicitly passes force=true. Safe to skip: invoices hold frozen snapshot
        // copies, so deleting the source transaction cannot change billed data.
        if (!force) {
            invoicedGuardHandler.handle(new TransactionPatchContext(null, entity));
        }
        transactionDao.delete(entity); // soft delete: one UPDATE, atomically in this transaction
        log.info("Soft-deleted transaction id={} force={}", id, force);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(int limit, int offset) {
        Page<TransactionEntity> page = transactionDao.findPage(limit, offset);
        // One IN query for the whole page's lines, grouped per transaction — no N+1.
        List<String> ids = page.getContent().stream().map(TransactionEntity::getId).toList();
        Map<String, List<TransactionLineEntity>> linesByTransaction = ids.isEmpty()
                ? Map.of()
                : transactionDao.findLinesByTransactionIds(ids).stream()
                        .collect(Collectors.groupingBy(TransactionLineEntity::getTransactionId));
        return PageResponse.<TransactionResponse>builder()
                .items(page.getContent().stream()
                        .map(entity -> transactionMapper.toTransactionResponse(
                                entity, linesByTransaction.getOrDefault(entity.getId(), List.of())))
                        .toList())
                .limit(limit)
                .offset(offset)
                .total(page.getTotalElements())
                .build();
    }

    private TransactionEntity findActiveByIdOrThrow(String id) {
        return transactionDao.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TRANSACTION_NOT_FOUND, id));
    }
}
