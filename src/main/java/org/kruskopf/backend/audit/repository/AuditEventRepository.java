package org.kruskopf.backend.audit.repository;

import org.kruskopf.backend.audit.entity.AuditEvent;
import org.kruskopf.backend.audit.entity.AuditEventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The audit trail's storage. Read, append, and one prune — in that order of
 * importance.
 * <p>
 * There is deliberately no update path and no delete other than
 * {@link #deleteOlderThan(LocalDateTime)}: the rows are append-only, and the
 * retention prune is the single exception (see docs/ARCHITECTURE.md, "Audit
 * trail").
 * <p>
 * The finders are ordered by {@code id} descending rather than by
 * {@code createdAt}: ids are unique and increase with time on an append-only
 * table, while two rows written in the same millisecond would leave a
 * timestamp-ordered page ambiguous. That also makes {@code beforeId} a cursor a
 * client can page with safely.
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    /** The newest rows, optionally from one type, optionally below a cursor. */
    List<AuditEvent> findByOrderByIdDesc(Pageable pageable);

    List<AuditEvent> findByIdLessThanOrderByIdDesc(Long beforeId, Pageable pageable);

    List<AuditEvent> findByEventTypeOrderByIdDesc(AuditEventType eventType, Pageable pageable);

    List<AuditEvent> findByEventTypeAndIdLessThanOrderByIdDesc(
            AuditEventType eventType, Long beforeId, Pageable pageable);

    /**
     * The retention prune. A bulk delete, so it does not load the rows it
     * removes — the trail is expected to grow without bound between prunes.
     * <p>
     * {@code @Transactional} is on the method rather than only on the caller:
     * a bulk delete needs a transaction of its own, and the prune is also
     * reachable from a test (or a future scheduled job) that has none.
     *
     * @return the number of rows removed
     */
    @Modifying
    @Transactional
    @Query("delete from AuditEvent event where event.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
