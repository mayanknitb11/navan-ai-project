package com.navan.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Shared identity + audit + optimistic-lock columns for EVERY table (DB_SCHEMA §2 rule 9):
 * {@code id} (assigned VARCHAR(40)), {@code version} (@Version), {@code created_at}, {@code updated_at}.
 *
 * Implements {@link Persistable} because ids are ASSIGNED, not database-generated: without it,
 * Spring Data would treat a new entity with a non-null id as "existing" and issue a merge
 * (SELECT + INSERT) instead of a plain INSERT. {@code isNew()} stays true until auditing stamps
 * {@code created_at} at first persist, so creates remain single INSERTs — critical for the
 * duplicate-create path, where the INSERT itself must hit the partial unique index.
 *
 * Construction is via the builder (@SuperBuilder — inheritance-aware); there are no setters here.
 * JPA populates fields by field access (the @Id is on the field), so persistence needs none.
 * Note: entities deliberately use @Getter, never Lombok @Data — generated equals/hashCode/toString
 * trigger lazy-collection loads and misbehave on Hibernate proxies.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity implements Persistable<String> {

    @Id
    @Column(length = 40)
    private String id;

    @Version
    private long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** True until the first persist stamps created_at; afterwards rows load as existing. */
    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}
