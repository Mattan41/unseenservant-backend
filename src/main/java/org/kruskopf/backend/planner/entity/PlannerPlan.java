package org.kruskopf.backend.planner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.entity.AuditedEntity;
import org.kruskopf.backend.user.entity.User;

/**
 * One plan: the question a group is answering in the planner, held together with
 * the candidate dates that answer it.
 * <p>
 * Any participant may start one ({@link #createdByUser}), because finding a date
 * is not an administrative act. {@link #title} is free text — "Session 12",
 * "Weekend trip", "When can we record?" — so the planner reads as a general
 * scheduling tool rather than a game-session feature.
 * <p>
 * {@link #status} starts {@code OPEN}; deciding sets it to {@code DECIDED} and
 * fills {@link #decidedDate}, which must be one of the plan's own dates.
 * Reopening puts it back to {@code OPEN} and clears the pointer, so the two
 * fields can never disagree.
 * <p>
 * {@link #version} gives the same two-layer optimistic concurrency as the ship
 * and journal pages: the service compares the client's token against the freshly
 * loaded value ({@code ConcurrentUpdateException}, 409), and Hibernate's
 * {@code UPDATE ... WHERE version = ?} catches a writer that committed in
 * between.
 * <p>
 * Membership is deliberately <em>not</em> a join table: the roster is the
 * campaign's participants minus the {@link PlannerMemberExclusion} rows of this
 * plan, which is what makes "every participant is in the plan unless they leave"
 * true without any bookkeeping.
 */
@Entity
@Table(name = "planner_plan", indexes = {
        @Index(name = "idx_planner_plan_campaign_status", columnList = "campaign_id,status")
})
public class PlannerPlan extends AuditedEntity {

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    /** Free text, trimmed. What the plan is for. */
    @Column(nullable = false, length = 200)
    private String title;

    /** Optional markdown source; the client renders it. */
    @Column(columnDefinition = "LONGTEXT")
    private String description = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlannerPlanStatus status = PlannerPlanStatus.OPEN;

    /** The chosen date once decided; cleared again on reopen. */
    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "decided_date_id")
    private PlannerDate decidedDate;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    /**
     * Optimistic-concurrency version, owned by Hibernate. Never assign this: the
     * service compares the client's token against the loaded value, and Hibernate
     * performs the increment on flush.
     */
    @Version
    @Column(nullable = false)
    private long version = 0;

    public PlannerPlan() {
    }

    public PlannerPlan(Campaign campaign, User createdByUser, String title, String description) {
        this.campaign = campaign;
        this.createdByUser = createdByUser;
        this.title = title;
        this.description = description == null ? "" : description;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public PlannerPlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlannerPlanStatus status) {
        this.status = status;
    }

    public PlannerDate getDecidedDate() {
        return decidedDate;
    }

    public void setDecidedDate(PlannerDate decidedDate) {
        this.decidedDate = decidedDate;
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

}
