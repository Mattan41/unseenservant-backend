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
import jakarta.persistence.Version;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.kruskopf.backend.user.entity.User;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * One candidate date of a {@link PlannerPlan} — "Saturday 14 March, 18:00–22:00,
 * bring dice".
 * <p>
 * Any participant may add one. The unique key on {@code (plan_id, date)} stops
 * the same day being offered twice, which is what keeps the colour markers
 * unambiguous: one row per day, so its answers are that day's answers.
 * {@code startTime} and {@code endTime} are optional, and a plan with no times at
 * all is a perfectly ordinary plan.
 * <p>
 * Deleting a date takes its answers with it ({@code ON DELETE CASCADE}); a plan
 * that had decided on it is left pointing at nothing
 * ({@code ON DELETE SET NULL}) — decided with the date gone, rather than broken.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "planner_date", uniqueConstraints = {
        @UniqueConstraint(name = "uq_planner_date_plan_date", columnNames = {"plan_id", "date"})
})
public class PlannerDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlannerPlan plan;

    @Column(name = "plan_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    /** Optional short note ("Sunday afternoon, bring dice"). */
    @Column(length = 500)
    private String note;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    /** Optimistic concurrency, as on the plan. Managed by Hibernate. */
    @Version
    @Column(nullable = false)
    private long version = 0;

    @CreatedBy
    private String createdBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @LastModifiedBy
    private String lastModifiedBy;

    public PlannerDate() {
    }

    public PlannerDate(PlannerPlan plan, User createdByUser, LocalDate date) {
        this.plan = plan;
        this.createdByUser = createdByUser;
        this.date = date;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public long getVersion() {
        return version;
    }

    /** For tests only. Production code must let Hibernate manage the version. */
    public void setVersion(long version) {
        this.version = version;
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
