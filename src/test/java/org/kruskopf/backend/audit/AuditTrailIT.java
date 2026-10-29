package org.kruskopf.backend.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.admin.service.AdminService;
import org.kruskopf.backend.audit.entity.AuditEvent;
import org.kruskopf.backend.audit.entity.AuditEventType;
import org.kruskopf.backend.audit.repository.AuditEventRepository;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The audit trail against a real database, where the two transaction policies
 * actually do something.
 * <p>
 * The point of this class is the row that has to outlive a rollback: a refused
 * privilege change throws out of a {@code @Transactional} method, so a row
 * written inside that transaction would vanish with it — and the refusal is
 * precisely what the trail exists to keep. {@code AuditEventStore.writeDetached}
 * is what makes it survive, and this is where that is proven rather than
 * asserted.
 * <p>
 * Deliberately not {@code @Transactional} (unlike the controller ITs): a test
 * transaction would swallow the rollback this is about, and would keep the
 * detached insert invisible to the assertions.
 */
@DisplayName("Audit trail Integration Tests")
class AuditTrailIT extends AbstractIntegrationTest {

    /** Suffix so two test methods can never collide on a unique column. */
    private static final AtomicInteger UNIQUE = new AtomicInteger();

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;
    private final AdminService adminService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    AuditTrailIT(AuditEventRepository auditEventRepository,
                 UserRepository userRepository,
                 AdminService adminService,
                 JdbcTemplate jdbcTemplate) {
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
        this.adminService = adminService;
        this.jdbcTemplate = jdbcTemplate;
    }

    private User actor;
    private User target;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        // This class is not transactional, so its rows outlive each test method.
        // Clearing the accounts is what makes the refusal below deterministic: it
        // needs `target` to be the *only* administrator, and a promotion in one
        // test must not become a second admin in the next.
        userRepository.deleteAll();

        // The only administrator, so demoting it is the refused case.
        target = aUser("audit-target", UserRole.ADMIN);
        // The account making the change. The service does not check its role -
        // the controller's guard does - so a plain account is enough here.
        actor = aUser("audit-actor", UserRole.USER);
    }

    private User aUser(String prefix, UserRole role) {
        String unique = prefix + "-it" + UNIQUE.incrementAndGet();
        User user = TestDataFactory.aUser(unique);
        user.setEmail(unique + "@example.com");
        user.setRole(role);
        return userRepository.save(user);
    }

    private List<AuditEvent> stored() {
        return auditEventRepository.findByOrderByIdDesc(PageRequest.of(0, 50));
    }

    @Test
    @DisplayName("Keeps a refused demotion after the change's transaction has rolled back")
    void keepsARefusedDemotionAcrossTheRollback() {
        // Read the id before the lambda: a throwing call inside it would be caught
        // by assertThatThrownBy and read as the refusal this test is about.
        long targetId = target.getId();

        assertThatThrownBy(() -> adminService.changeUserRole(targetId, UserRole.USER, actor))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("last administrator");

        List<AuditEvent> rows = stored();
        assertThat(rows).hasSize(1);

        AuditEvent refusal = rows.getFirst();
        assertThat(refusal.getEventType()).isEqualTo(AuditEventType.ROLE_CHANGE_REFUSED);
        assertThat(refusal.getActorId()).isEqualTo(actor.getId());
        assertThat(refusal.getActorName()).isEqualTo(actor.getUserName());
        assertThat(refusal.getTargetId()).isEqualTo(target.getId());
        assertThat(refusal.getTargetName()).isEqualTo(target.getUserName());
        assertThat(refusal.getDetail()).isEqualTo("the last administrator cannot be demoted");
        assertThat(refusal.getCreatedAt()).isNotNull();
        // The role was not, in fact, changed.
        assertThat(userRepository.findById(target.getId()).orElseThrow().getRole())
                .isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("Records a role change with the role it came from")
    void recordsARoleChange() {
        User member = aUser("audit-member", UserRole.USER);

        adminService.changeUserRole(member.getId(), UserRole.ADMIN, actor);

        List<AuditEvent> rows = stored();
        assertThat(rows).hasSize(1);

        AuditEvent change = rows.getFirst();
        assertThat(change.getEventType()).isEqualTo(AuditEventType.ROLE_CHANGED);
        assertThat(change.getActorName()).isEqualTo(actor.getUserName());
        assertThat(change.getTargetName()).isEqualTo(member.getUserName());
        assertThat(change.getDetail()).isEqualTo("USER -> ADMIN");
    }

    @Test
    @DisplayName("Records a whitelist change with the address as the target")
    void recordsAWhitelistChange() {
        adminService.addWhitelist(actor, "New-Person@Example.com", "USER");

        List<AuditEvent> rows = stored();
        assertThat(rows).hasSize(1);

        AuditEvent added = rows.getFirst();
        assertThat(added.getEventType()).isEqualTo(AuditEventType.WHITELIST_ADDED);
        // The address, lower-cased as stored, and not an account id.
        assertThat(added.getTargetName()).isEqualTo("new-person@example.com");
        assertThat(added.getTargetId()).isNull();
        assertThat(added.getDetail()).isEqualTo("role USER");
    }

    @Test
    @DisplayName("Prunes the rows past the retention period, and only those")
    void prunesOnlyRowsPastRetention() {
        // Written by hand rather than through the service, because the timestamp
        // is the input the prune reads and it cannot be put in the past through
        // the API: createdAt comes from the auditing clock.
        saveAt(AuditEventType.LOGIN_SUCCEEDED, "elder", 400);
        saveAt(AuditEventType.LOGIN_SUCCEEDED, "recent", 10);

        new AuditRetentionRunner(auditEventRepository, 365).run();

        List<AuditEvent> rows = stored();
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getActorName()).isEqualTo("recent");
    }

    @Test
    @DisplayName("Keeps every row when retention is switched off")
    void keepsEveryRowWhenRetentionIsOff() {
        saveAt(AuditEventType.LOGIN_SUCCEEDED, "elder", 400);

        new AuditRetentionRunner(auditEventRepository, 0).run();

        assertThat(stored()).hasSize(1);
    }

    /**
     * Writes a row with a timestamp in the past.
     * <p>
     * Through SQL rather than the service, because {@code createdAt} is written
     * by the auditing listener on every insert and cannot be aged through the
     * API — while the timestamp is exactly the input the prune reads. The insert
     * also pins the migration's column names against what the entity expects.
     */
    private void saveAt(AuditEventType type, String actorName, int daysAgo) {
        jdbcTemplate.update(
                "insert into audit_event (event_type, actor_name, detail, created_at, updated_at) "
                        + "values (?, ?, ?, ?, ?)",
                type.name(), actorName, "password",
                LocalDateTime.now(ZoneOffset.UTC).minusDays(daysAgo),
                LocalDateTime.now(ZoneOffset.UTC));
    }
}
