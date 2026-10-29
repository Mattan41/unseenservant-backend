package org.kruskopf.backend.audit;

import org.kruskopf.backend.audit.entity.AuditEvent;
import org.kruskopf.backend.audit.repository.AuditEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one place an audit row is written, and the seam where the two transaction
 * policies live.
 * <p>
 * It exists as its own bean rather than as two methods on {@link AuditService}
 * because of how Spring's transaction interceptor works: a call from one method
 * of a bean to another of the same bean does not go through the proxy, so an
 * annotation on the callee would be silently ignored. The distinction below is
 * the whole reason the class is separate, so it must not be faked by
 * self-invocation.
 */
@Component
public class AuditEventStore {

    private final AuditEventRepository repository;

    public AuditEventStore(AuditEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Writes a row about a <em>change</em>, joining the caller's transaction.
     * <p>
     * Joining is the point: if the trail cannot be written, the change it
     * describes must not be committed either. An unaudited role change is worse
     * than a refused one.
     */
    @Transactional
    public void write(AuditEvent event) {
        repository.save(event);
    }

    /**
     * Writes a row about an <em>attempt or refusal</em>, in a transaction of its
     * own, so it survives whatever happens to the caller's.
     * <p>
     * Two cases need this. A login has no surrounding transaction at all, so the
     * row simply commits on its own. A refused role change is thrown out of a
     * {@code @Transactional} method, which rolls that transaction back — a row
     * written inside it would vanish with it, and the refusal would leave no
     * trace, which is exactly the case the row exists for.
     * <p>
     * Suspending the caller's transaction means holding a second pooled
     * connection for the duration of one insert. That is a deliberate trade at
     * this scale (see docs/ARCHITECTURE.md, "Concurrency" and "Audit trail").
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeDetached(AuditEvent event) {
        repository.save(event);
    }
}
