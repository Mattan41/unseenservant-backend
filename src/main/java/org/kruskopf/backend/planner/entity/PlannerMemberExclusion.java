package org.kruskopf.backend.planner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.kruskopf.backend.user.entity.User;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * A member who is not in a {@link PlannerPlan}, and why.
 * <p>
 * The roster is the campaign's participants minus these rows, so a plan starts
 * with everybody in it and needs no rows of its own. Two writes produce one of
 * these, and only one of them is the member's to undo:
 * <ul>
 *   <li>a member left the plan themselves — {@code removedByOwner} is false, and
 *       they may rejoin;</li>
 *   <li>the campaign owner removed them — {@code removedByOwner} is true, and
 *       only the owner puts them back.</li>
 * </ul>
 * Without that distinction a member could walk back into a plan the owner had
 * taken them out of, which would make the owner's control meaningless.
 * <p>
 * The row is what "not attending this time" means; it is deliberately not a
 * deletion of their answers, so rejoining restores them.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "planner_member_exclusion", uniqueConstraints = {
        @UniqueConstraint(name = "uq_planner_exclusion_plan_user", columnNames = {"plan_id", "user_id"})
})
public class PlannerMemberExclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlannerPlan plan;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Who removed them, by username (the audit column convention here). */
    @Column(name = "removed_by", length = 255)
    private String removedBy;

    /** True when the campaign owner removed them, false when they left themselves. */
    @Column(name = "removed_by_owner", nullable = false)
    private boolean removedByOwner;

    @CreatedBy
    private String createdBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @LastModifiedBy
    private String lastModifiedBy;

    public PlannerMemberExclusion() {
    }

    public PlannerMemberExclusion(PlannerPlan plan, User user, String removedBy, boolean removedByOwner) {
        this.plan = plan;
        this.user = user;
        this.removedBy = removedBy;
        this.removedByOwner = removedByOwner;
    }

    public Long getId() {
        return id;
    }

    /** For tests only: ids are assigned by the database. */
    public void setId(Long id) {
        this.id = id;
    }

    public PlannerPlan getPlan() {
        return plan;
    }

    public void setPlan(PlannerPlan plan) {
        this.plan = plan;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(String removedBy) {
        this.removedBy = removedBy;
    }

    public boolean isRemovedByOwner() {
        return removedByOwner;
    }

    public void setRemovedByOwner(boolean removedByOwner) {
        this.removedByOwner = removedByOwner;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
}