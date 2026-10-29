package org.kruskopf.backend.audit;

import org.kruskopf.backend.audit.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Keeps the audit trail to its retention period.
 * <p>
 * The rows are personal data — a username, an address, where someone signed in
 * from — so they are not kept forever, and the period is a documented number
 * rather than a growing table ({@code app.audit.retention-days}). The prune runs
 * once at startup, which is the same shape {@code WhitelistLoader} uses and
 * needs no scheduler: at this scale the table gains a handful of rows a day, so
 * "daily, at boot" and "daily, at 03:00" differ by nothing an operator can
 * observe. It is the one delete in the application (see
 * docs/ARCHITECTURE.md, "Audit trail").
 * <p>
 * A non-positive period disables the prune instead of deleting everything, so a
 * mis-set environment variable cannot wipe the trail.
 */
@Component
public class AuditRetentionRunner implements CommandLineRunner {

    private static final Logger auditLog = LoggerFactory.getLogger("audit");

    private final AuditEventRepository repository;

    /** Days a row is kept. {@code 0} or less keeps every row. */
    private final int retentionDays;

    public AuditRetentionRunner(AuditEventRepository repository,
                                @Value("${app.audit.retention-days:365}") int retentionDays) {
        this.repository = repository;
        this.retentionDays = retentionDays;
    }

    @Override
    public void run(String... args) {
        if (retentionDays <= 0) {
            auditLog.info("Audit retention is disabled (app.audit.retention-days={}) - "
                    + "every audit row is kept.", retentionDays);
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(retentionDays);
        // The delete owns its transaction (AuditEventRepository.deleteOlderThan),
        // so this method needs none of its own.
        int removed = repository.deleteOlderThan(cutoff);
        auditLog.info("Audit retention: removed {} row(s) older than {} day(s) (before {}).",
                removed, retentionDays, cutoff);
    }
}
