package com.navan.commerce.repository;

import com.navan.commerce.domain.LineCategory;
import com.navan.commerce.entity.TransactionLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Key-based access to transaction money lines (flat entities — no ORM associations).
 * Children of soft-deleted transactions are simply never queried (parent filter first).
 */
public interface TransactionLineRepository extends JpaRepository<TransactionLineEntity, String> {

    List<TransactionLineEntity> findByTransactionId(String transactionId);

    /** One IN query for a whole page of transactions — no N+1. */
    List<TransactionLineEntity> findByTransactionIdIn(Collection<String> transactionIds);

    /** Replace-all support: remove one category's rows for a transaction. */
    void deleteByTransactionIdAndCategory(String transactionId, LineCategory category);
}
