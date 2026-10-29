package org.kruskopf.backend.audit.dto;

import org.kruskopf.backend.audit.entity.AuditEvent;

import java.time.LocalDateTime;

/**
 * One audit row, as the Logs view reads it.
 *
 * @param id         row id, and the cursor a client pages with ({@code beforeId})
 * @param type       the {@code AuditEventType} name — the client labels and
 *                   badges on it, so a rename is an API change
 * @param occurredAt when it happened, as the UTC wall clock the rest of the API
 *                   uses (`createdAt`; see docs/INTEGRATION_CONTRACT.md)
 * @param actorId    the account that did it, or {@code null} when nobody with an
 *                   account did (a failed login)
 * @param actorName  the actor's username at the time, or the attempted username
 * @param targetId   the account acted on, or {@code null}
 * @param targetName the target's username or email at the time, or {@code null}
 * @param detail     the short sentence the view shows
 * @param clientIp   where it came from, or {@code null} outside a request
 */
public record AuditEventDTO(
        Long id,
        String type,
        LocalDateTime occurredAt,
        Long actorId,
        String actorName,
        Long targetId,
        String targetName,
        String detail,
        String clientIp) {

    public static AuditEventDTO from(AuditEvent event) {
        return new AuditEventDTO(
                event.getId(),
                event.getEventType().name(),
                event.getCreatedAt(),
                event.getActorId(),
                event.getActorName(),
                event.getTargetId(),
                event.getTargetName(),
                event.getDetail(),
                event.getClientIp());
    }
}
