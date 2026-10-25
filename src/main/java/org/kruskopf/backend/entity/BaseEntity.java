package org.kruskopf.backend.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * The part of an entity that is not about its own subject: a generated id, and the two
 * timestamps Spring Data writes for it.
 * <p>
 * A {@code @MappedSuperclass}, so nothing here becomes a table of its own — every
 * subclass maps these fields onto its own table exactly as it did when the fields were
 * written out in each entity. That is also why the audit columns are split in two:
 * {@link AuditedEntity} adds the two {@code by} columns, and an entity that has no such
 * columns simply does not extend it. Inheriting a field a table does not have would
 * have meant a migration.
 * <p>
 * {@code @EntityListeners} is declared here and applies to every subclass (a mapped
 * superclass's listeners are inherited), so the timestamps keep being filled in. The
 * listener is the only writer of {@link #createdAt} and {@link #updatedAt}: an entity
 * must not stamp them, and what a transient instance holds is not what its row will
 * hold. Both are filled from {@code JpaConfig}'s {@code DateTimeProvider}, which is UTC.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    /** For tests only: ids are assigned by the database. */
    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
