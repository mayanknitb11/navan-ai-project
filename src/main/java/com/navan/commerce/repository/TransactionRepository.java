package com.navan.commerce.repository;

import com.navan.commerce.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data gateway for transactions. Soft delete (D11) is applied HERE: every query carries
 * {@code deletedAtIsNull}, so "find" always means "find active" (deleted = does not exist) and
 * the predicate matches the partial indexes exactly. Never call the inherited delete() —
 * soft delete lives in the DAO ({@code setDeletedAt} + save).
 */
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    Optional<TransactionEntity> findByIdAndDeletedAtIsNull(String id);

    /** Used for the duplicate-conflict lookup after a unique-index violation. */
    Optional<TransactionEntity> findByExternalIdAndDeletedAtIsNull(String externalId);

    /** Invoice creation loads only ACTIVE transactions — a deleted id resolves to 404 (D12). */
    List<TransactionEntity> findAllByIdInAndDeletedAtIsNull(Collection<String> ids);

    Page<TransactionEntity> findAllByDeletedAtIsNull(Pageable pageable);
}
