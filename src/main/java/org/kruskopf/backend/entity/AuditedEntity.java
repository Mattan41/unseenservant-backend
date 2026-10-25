package org.kruskopf.backend.entity;

import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

/**
 * An entity whose table also records <em>who</em> wrote the row: the {@code by} half of
 * the audit trail, on top of {@link BaseEntity}'s timestamps.
 * <p>
 * Deliberately its own class rather than part of {@code BaseEntity}: several entities
 * have no {@code created_by} / {@code last_modified_by} columns, and moving the fields
 * down a level is what lets every entity inherit exactly the columns it already has.
 */
@MappedSuperclass
public abstract class AuditedEntity extends BaseEntity {

    /**
     * Who wrote the row, by username. The column is {@code created_by} for every
     * entity — the convention this app follows even where the semantic is narrower
     * (an add-on switch reads as "who enabled it", and is stored in the same column).
     */
    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
}
