package org.kruskopf.backend.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.kruskopf.backend.entity.BaseEntity;
import org.kruskopf.backend.user.entity.User;

/**
 * One row of the audit trail: what happened, to whom, by whom, and when.
 * <p>
 * The row is <b>append-only</b>. There is no setter on any field, the repository
 * offers no update or delete beyond the retention prune, and no endpoint reaches
 * a row other than by reading it — an audit entry whose answer can be edited is
 * not worth keeping.
 * <p>
 * The names are copied at write time rather than joined at read time, and there
 * is deliberately no foreign key to {@code users} (see
 * {@code V5__audit_event.sql}): the row has to keep reading correctly after the
 * account it names is renamed or deleted.
 * <p>
 * The four factories are the whole API of this class, one per shape a caller can
 * actually have. They exist instead of a long constructor so that a call site
 * cannot silently swap the actor for the target — the one mistake a positional
 * argument list of seven strings invites.
 * <p>
 * The timestamp is not a field of its own: {@link BaseEntity}'s {@code createdAt}
 * is the moment of the event, filled by JPA auditing from {@code JpaConfig}'s UTC
 * clock. {@code updatedAt} is inherited and never meaningfully changes, which is
 * the price of not inventing a second base class for one table.
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private AuditEventType eventType;

    /** The account that did it, or {@code null} when there was no account (a failed login). */
    @Column(name = "actor_id")
    private Long actorId;

    /** The actor's username at the time — a copy, so a later rename does not rewrite this row. */
    @Column(name = "actor_name", length = 255)
    private String actorName;

    /** The account that was acted on, or {@code null} (a login, or a whitelist address). */
    @Column(name = "target_id")
    private Long targetId;

    /** The target's name at the time: a username, or a whitelist email address. */
    @Column(name = "target_name", length = 255)
    private String targetName;

    /** The short human sentence the Logs view shows next to the event. */
    @Column(length = 500)
    private String detail;

    /** Where it came from, per {@code ClientIpResolver}; {@code null} outside a request. */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    protected AuditEvent() {
    }

    private AuditEvent(AuditEventType eventType, Long actorId, String actorName,
                       Long targetId, String targetName, String detail, String clientIp) {
        this.eventType = eventType;
        this.actorId = actorId;
        this.actorName = actorName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.detail = detail;
        this.clientIp = clientIp;
    }

    /** An account acting on nothing: a login, or the account it just created. */
    public static AuditEvent by(AuditEventType eventType, User actor, String detail, String clientIp) {
        return new AuditEvent(eventType, actor.getId(), actor.getUserName(),
                null, null, detail, clientIp);
    }

    /** An administrator acting on an account. */
    public static AuditEvent byOn(AuditEventType eventType, User actor, User target,
                                  String detail, String clientIp) {
        return new AuditEvent(eventType, actor.getId(), actor.getUserName(),
                target.getId(), target.getUserName(), detail, clientIp);
    }

    /** An administrator acting on a whitelist address, which is not an account. */
    public static AuditEvent byOnEmail(AuditEventType eventType, User actor, String email,
                                       String detail, String clientIp) {
        return new AuditEvent(eventType, actor.getId(), actor.getUserName(),
                null, email, detail, clientIp);
    }

    /**
     * Nobody with an account acted: a failed login, a throttle, or a provider
     * login refused for someone who has no account here.
     *
     * @param actorName  the username that was attempted, or {@code null}
     * @param targetName the address the provider offered, or {@code null}
     */
    public static AuditEvent anonymous(AuditEventType eventType, String actorName, String targetName,
                                       String detail, String clientIp) {
        return new AuditEvent(eventType, null, actorName, null, targetName, detail, clientIp);
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getDetail() {
        return detail;
    }

    public String getClientIp() {
        return clientIp;
    }
}
