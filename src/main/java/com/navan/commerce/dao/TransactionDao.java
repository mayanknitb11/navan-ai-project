package com.navan.commerce.dao;

import com.navan.commerce.constant.DatabaseConstants;
import com.navan.commerce.domain.LineCategory;
import com.navan.commerce.entity.TransactionEntity;
import com.navan.commerce.entity.TransactionLineEntity;
import com.navan.commerce.repository.InvoiceLineRepository;
import com.navan.commerce.repository.TransactionLineRepository;
import com.navan.commerce.repository.TransactionRepository;
import com.navan.commerce.util.OffsetPageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * All transaction data-access in one place (services never touch repositories — package rule,
 * which is also the soft-delete safeguard: this class is the ONLY query-composition point, so
 * the {@code deletedAt IS NULL} filter cannot be forgotten anywhere else). All reads return
 * active rows only.
 *
 * Entities are flat (no ORM associations): parents and children are persisted/fetched with
 * explicit key queries. Atomicity comes from the caller's {@code @Transactional} boundary.
 */
@Component
@RequiredArgsConstructor
public class TransactionDao {

    /** Stable page order backed by idx_transactions_created_id. */
    private static final Sort PAGE_SORT = Sort.by(DatabaseConstants.SORT_CREATED_AT).ascending()
            .and(Sort.by(DatabaseConstants.SORT_ID).ascending());

    private final TransactionRepository transactionRepository;
    private final TransactionLineRepository transactionLineRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * Header first (flushed immediately so a unique-index violation surfaces HERE, before any
     * child insert), then the line rows. One commit by the caller's transaction — all or none.
     */
    public TransactionEntity createWithLines(TransactionEntity entity, List<TransactionLineEntity> lines) {
        TransactionEntity saved = transactionRepository.saveAndFlush(entity);
        transactionLineRepository.saveAll(lines);
        return saved;
    }

    public Optional<TransactionEntity> findById(String id) {
        return transactionRepository.findByIdAndDeletedAtIsNull(id);
    }

    public List<TransactionEntity> findAllByIds(Collection<String> ids) {
        return transactionRepository.findAllByIdInAndDeletedAtIsNull(ids);
    }

    public List<TransactionLineEntity> findLines(String transactionId) {
        return transactionLineRepository.findByTransactionId(transactionId);
    }

    /** One IN query for a whole page of transactions — no N+1. */
    public List<TransactionLineEntity> findLinesByTransactionIds(Collection<String> transactionIds) {
        return transactionLineRepository.findByTransactionIdIn(transactionIds);
    }

    /** Replace-all within one category: bulk-delete that category's rows, insert the new ones. */
    public void replaceLines(String transactionId, LineCategory category, List<TransactionLineEntity> newLines) {
        transactionLineRepository.deleteByTransactionIdAndCategory(transactionId, category);
        transactionLineRepository.saveAll(newLines);
    }

    public boolean existsOnAnyActiveInvoice(String transactionId) {
        return invoiceLineRepository.existsOnAnyActiveInvoice(transactionId);
    }

    public Page<TransactionEntity> findPage(int limit, int offset) {
        return transactionRepository.findAllByDeletedAtIsNull(new OffsetPageRequest(limit, offset, PAGE_SORT));
    }

    /**
     * Soft delete via plain JPA CRUD: stamp deleted_at and save(). The UPDATE carries the
     * @Version check, so a concurrent PATCH/DELETE loser still gets 409. Line rows are retained
     * with their parent (financial audit trail) and become unreachable via the API.
     */
    public void delete(TransactionEntity entity) {
        entity.setDeletedAt(Instant.now());
        transactionRepository.save(entity);
    }

    /**
     * Looks up the winner of a duplicate-insert race in a FRESH transaction. Required because a
     * constraint violation aborts the current Postgres transaction — reading inside it would fail.
     * A plain SELECT never blocks on the winner's speculative insert; it sees the committed row.
     */
    public Optional<String> findActiveIdByExternalIdInNewTransaction(String externalId) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        def.setReadOnly(true);
        return new TransactionTemplate(transactionManager, def)
                .execute(status -> transactionRepository.findByExternalIdAndDeletedAtIsNull(externalId).map(TransactionEntity::getId));
    }
}
