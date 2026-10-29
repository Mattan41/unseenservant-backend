package org.kruskopf.backend.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.kruskopf.backend.audit.dto.AuditEventDTO;
import org.kruskopf.backend.audit.entity.AuditEvent;
import org.kruskopf.backend.audit.entity.AuditEventType;
import org.kruskopf.backend.audit.repository.AuditEventRepository;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.ratelimit.ClientIpResolver;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The audit trail: the events the application records about accounts and
 * administration, and the read the Logs view makes.
 * <p>
 * Two rules shape the whole class, and both are stated where they are applied:
 * <ul>
 *   <li><b>a change joins the caller's transaction</b> — an unwritable row vetoes
 *       the change, so nothing privileged happens unaudited
 *       ({@link AuditEventStore#write});</li>
 *   <li><b>an attempt never fails the request</b> — the row is written in its own
 *       transaction and a failure is only a warning, because a full disk must not
 *       turn a valid login into a 500 ({@link AuditEventStore#writeDetached}).</li>
 * </ul>
 * The methods below are named after the event rather than taking a type and a
 * fistful of nullable strings, so a call site reads as the sentence it records
 * and cannot swap the actor for the target.
 * <p>
 * Every event is also mirrored to the {@code audit} logger as one {@code key=value}
 * line. The table is the readable trail; the line is the half that survives the
 * database being unavailable, is greppable in {@code docker logs}, and can be
 * routed to a file or a collector later without touching this class. The two are
 * deliberately redundant — see docs/ARCHITECTURE.md ("Audit trail").
 * <p>
 * Only user- and administration-related events are recorded. Campaigns,
 * messages, characters and the rest are not: that is a scope decision, not an
 * oversight (docs/ARCHITECTURE.md).
 */
@Service
public class AuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("audit");

    private static final String DASH = "-";

    private final AuditEventStore store;
    private final AuditEventRepository repository;

    /** How many rows a page may hold; a caller asking for more is clamped to it. */
    private final int maxPageSize;

    /** How many rows a caller that does not ask for a page gets. */
    private final int defaultPageSize;

    public AuditService(AuditEventStore store,
                        AuditEventRepository repository,
                        @Value("${app.audit.max-page-size:500}") int maxPageSize,
                        @Value("${app.audit.default-page-size:100}") int defaultPageSize) {
        this.store = store;
        this.repository = repository;
        // A cap below 1 cannot be honoured, and Math.clamp refuses min > max, so a
        // misconfigured property is read as 1 instead of failing every request.
        this.maxPageSize = maxPageSize < 1 ? 1 : maxPageSize;
        this.defaultPageSize = defaultPageSize;
    }

    // ------------------------------------------------------------------
    // Changes: recorded in the caller's transaction, so they can veto it.
    // ------------------------------------------------------------------

    /** A first OAuth login created the account. */
    public void accountCreated(User user, ProviderType provider, UserRole role) {
        recordChange(AuditEvent.by(AuditEventType.ACCOUNT_CREATED, user,
                "provider " + provider + ", role " + role, clientIp()));
    }

    /** An administrator changed an account's role. */
    public void roleChanged(User actor, User target, UserRole from, UserRole to) {
        recordChange(AuditEvent.byOn(AuditEventType.ROLE_CHANGED, actor, target,
                from + " -> " + to, clientIp()));
    }

    /** An address was admitted to the whitelist, or a revoked row was re-admitted. */
    public void whitelistAdded(User actor, String email, UserRole role) {
        recordChange(AuditEvent.byOnEmail(AuditEventType.WHITELIST_ADDED, actor, email,
                "role " + role, clientIp()));
    }

    /**
     * A whitelist row was re-roled and/or re-activated.
     *
     * @param role   the role this request asked for, or {@code null}
     * @param active the active flag this request carried, or {@code null}
     */
    public void whitelistUpdated(User actor, String email, UserRole role, Boolean active) {
        recordChange(AuditEvent.byOnEmail(AuditEventType.WHITELIST_UPDATED, actor, email,
                describe(role, active), clientIp()));
    }

    /** An address was revoked: the row stays, but it stops admitting logins. */
    public void whitelistRevoked(User actor, String email) {
        recordChange(AuditEvent.byOnEmail(AuditEventType.WHITELIST_REVOKED, actor, email,
                "logins refused", clientIp()));
    }

    // ------------------------------------------------------------------
    // Attempts and refusals: recorded detached, and never fail the request.
    // ------------------------------------------------------------------

    /**
     * A correct password, or a completed OAuth handshake.
     *
     * @param method how the caller signed in — {@code password}, or the provider
     */
    public void loginSucceeded(User user, String method) {
        quietly(AuditEvent.by(AuditEventType.LOGIN_SUCCEEDED, user, method, clientIp()));
    }

    /** A wrong password, or a username that does not exist. */
    public void loginFailed(String attemptedUsername) {
        quietly(AuditEvent.anonymous(AuditEventType.LOGIN_FAILED, attemptedUsername, null,
                "wrong password or unknown username", clientIp()));
    }

    /** The login throttle answered 429 before the password was even checked. */
    public void loginThrottled(String attemptedUsername) {
        quietly(AuditEvent.anonymous(AuditEventType.LOGIN_THROTTLED, attemptedUsername, null,
                "too many attempts", clientIp()));
    }

    /**
     * A provider login was refused before any account was created or touched: an
     * unverified email, or an address that is not on the whitelist. This row is
     * what answers "why can't this person log in?" — the redirect to
     * {@code /under-construction} says nothing.
     */
    public void oauthRefused(String email, String reason) {
        quietly(AuditEvent.anonymous(AuditEventType.OAUTH_REFUSED, null, email, reason, clientIp()));
    }

    /** The self or last-administrator guard refused a change. */
    public void roleChangeRefused(User actor, User target, String reason) {
        quietly(AuditEvent.byOn(AuditEventType.ROLE_CHANGE_REFUSED, actor, target, reason, clientIp()));
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    /**
     * The newest rows first, optionally from one type, optionally below a cursor.
     * <p>
     * {@code limit} is clamped rather than refused: this backs a list view, so a
     * client asking for more than {@code app.audit.max-page-size} gets a page
     * instead of an error. {@code beforeId} is the id of the last row of the
     * previous page (the DTO carries it), which is a stable cursor because ids
     * increase with time on an append-only table.
     *
     * @param type     only this event type, or {@code null} for everything
     * @param beforeId only rows older than this id, or {@code null} for the newest
     * @param limit    how many rows at most; {@code null} takes the default
     */
    public List<AuditEventDTO> recent(AuditEventType type, Long beforeId, Integer limit) {
        Pageable page = PageRequest.of(0, clampLimit(limit));

        List<AuditEvent> events;
        if (type == null && beforeId == null) {
            events = repository.findByOrderByIdDesc(page);
        } else if (type == null) {
            events = repository.findByIdLessThanOrderByIdDesc(beforeId, page);
        } else if (beforeId == null) {
            events = repository.findByEventTypeOrderByIdDesc(type, page);
        } else {
            events = repository.findByEventTypeAndIdLessThanOrderByIdDesc(type, beforeId, page);
        }

        return events.stream().map(AuditEventDTO::from).toList();
    }

    /**
     * Parses an {@code AuditEventType} name from a query parameter. Mirrors
     * {@code AdminService.parseRole} — a bad filter is named, not ignored.
     *
     * @throws InvalidRequestException when the value is missing or unknown
     */
    public static AuditEventType parseType(String type) {
        if (type == null || type.isBlank()) {
            throw new InvalidRequestException("An event type is required (" + typeNames() + ").");
        }
        try {
            return AuditEventType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException _) {
            throw new InvalidRequestException("Unknown event type '" + type + "' (" + typeNames() + ").");
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /** A change: the app log line first, then the row in the caller's transaction. */
    private void recordChange(AuditEvent event) {
        mirror(event);
        store.write(event);
    }

    /** An attempt: the app log line first, then a row that cannot fail the request. */
    private void quietly(AuditEvent event) {
        mirror(event);
        try {
            store.writeDetached(event);
        } catch (RuntimeException ex) {
            // No claim about the mirror line: it is only written when this logger
            // logs INFO, which the guard in mirror() honours.
            auditLog.warn("AUDIT {} could not be stored: {}", event.getEventType(), ex.getMessage());
        }
    }

    /**
     * The one line every audit event also writes to the {@code audit} logger.
     * <p>
     * Guarded, and assembled into locals before the call: java:S2629 is right that
     * arguments built from calls are evaluated whether anybody reads the
     * line or not, and the guard means silencing the logger
     * ({@code logging.level.audit=WARN}) costs nothing at all.
     */
    private static void mirror(AuditEvent event) {
        if (!auditLog.isInfoEnabled()) {
            return;
        }

        String type = event.getEventType().name();
        String actor = orDash(event.getActorId(), event.getActorName());
        String target = orDash(event.getTargetId(), event.getTargetName());
        String detail = event.getDetail();
        String clientIp = event.getClientIp();

        auditLog.info("AUDIT type={} actor={} target={} detail={} ip={}",
                type, actor, target, detail, clientIp == null ? DASH : clientIp);
    }

    private static String orDash(Long id, String name) {
        if (name == null) {
            return DASH;
        }
        return id == null ? name : id + ":" + name;
    }

    /**
     * The sentence a whitelist change carries: the role the caller asked for, and
     * whether the address was re-admitted or revoked.
     * <p>
     * Both arguments are the <em>request's</em> values rather than the row's
     * resulting state, which is why either can be absent — a PATCH may send a
     * role, an active flag, or both. The outcome alone could not tell a
     * re-admission from a no-op, and a trail must not claim one that did not
     * happen.
     */
    private static String describe(UserRole role, Boolean active) {
        boolean reAdmitted = Boolean.TRUE.equals(active);

        if (role == null) {
            return reAdmitted ? "re-admitted" : "revoked";
        }

        String state = "";
        if (active != null) {
            state = reAdmitted ? ", re-admitted" : ", revoked";
        }
        return "role " + role + state;
    }

    private int clampLimit(Integer limit) {
        int requested = limit == null ? defaultPageSize : limit;
        return Math.clamp(requested, 1, maxPageSize);
    }

    private static String typeNames() {
        return Arrays.stream(AuditEventType.values()).map(Enum::name).collect(Collectors.joining(", "));
    }

    /**
     * Where the event came from, resolved the same way the login throttle
     * resolves it — including the {@code CF-Connecting-IP} trust chain
     * {@link ClientIpResolver} documents.
     * <p>
     * Read from the current request rather than passed in, because half the call
     * sites are domain services that have no business taking a
     * {@code HttpServletRequest}: threading one through
     * {@code AdminService.changeUserRole} so a log line could name an address
     * would be the tail wagging the dog.
     *
     * @return the address, or {@code null} outside a request (a test, the
     *   retention prune) — never throws for that reason
     */
    private static String clientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            return ClientIpResolver.resolve(request);
        }
        return null;
    }
}
