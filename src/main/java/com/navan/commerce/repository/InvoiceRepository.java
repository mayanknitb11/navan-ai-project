package com.navan.commerce.repository;

import com.navan.commerce.entity.InvoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data gateway for invoices. Soft delete (D11) is applied HERE: every query carries
 * {@code deletedAtIsNull}. Never call the inherited delete() — soft delete lives in the DAO.
 */
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, String> {

    Optional<InvoiceEntity> findByIdAndDeletedAtIsNull(String id);

    Page<InvoiceEntity> findAllByDeletedAtIsNull(Pageable pageable);
}
