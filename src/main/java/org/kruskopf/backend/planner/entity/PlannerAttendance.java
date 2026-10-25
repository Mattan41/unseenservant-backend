package org.kruskopf.backend.planner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * One member's answer to one {@link PlannerDate}.
 * <p>
 * The unique key on {@code (plan_date_id, user_id)} is the whole rule: one row per
 * member per date, updated in place when they change their mind. A member with no
 * row has not answered — which is a state the client shows and the colour marker
 * counts against, so {@link PlannerAttendanceState} has no value for it.
 * <p>
 * Deliberately without a version token: the row is the caller's own answer, so
 * there is nothing of somebody else's to overwrite — the last of a member's own
 * two open tabs wins, and that is the right answer for a vote.
 * <p>
 * Answers survive a member leaving the plan (the exclusion row is what removes
 * them from the roster), so rejoining restores what they said — the same
 * reasoning as a journal surviving a participant leaving the campaign.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "planner_attendance", uniqueConstraints = {
        @UniqueConstraint(name = "uq_planner_attendance_date_user", columnNames = {"plan_date_id", "user_id"})
})
public class PlannerAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "plan_date_id", nullable = false)
    private PlannerDate planDate;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PlannerAttendanceState state;

    @CreatedBy
    private String createdBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @LastModifiedBy
    private String lastModifiedBy;

    public PlannerAttendance() {
    }

    public PlannerAttendance(PlannerDate planDate, User user, PlannerAttendanceState state) {
        this.planDate = planDate;
        this.user = user;
        this.state = state;
    }

    public Long getId() {
        return id;
    }

    /** For tests only: ids are assigned by the database. */
    public void setId(Long id) {
        this.id = id;
    }

    public PlannerDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(PlannerDate planDate) {
        this.planDate = planDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PlannerAttendanceState getState() {
        return state;
    }

    public void setState(PlannerAttendanceState state) {
        this.state = state;
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
