package org.kruskopf.backend.audit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.audit.AuditEventStore;
import org.kruskopf.backend.audit.AuditService;
import org.kruskopf.backend.audit.dto.AuditEventDTO;
import org.kruskopf.backend.audit.entity.AuditEvent;
import org.kruskopf.backend.audit.entity.AuditEventType;
import org.kruskopf.backend.audit.repository.AuditEventRepository;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The audit trail's policy: which of the two transaction paths an event takes,
 * and what happens when the store is unavailable.
 * <p>
 * The transaction semantics themselves (that the detached write really survives a
 * rolled-back caller) are a Spring concern, not a unit-test one — {@code
 * AuditTrailIT} proves those against a real database.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Unit Tests")
class AuditServiceTest {

    @Mock
    private AuditEventStore store;

    @Mock
    private AuditEventRepository repository;

    private AuditService auditService;

    private User actor;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(store, repository, 500, 100);

        actor = TestDataFactory.aUser("actor");
        actor.setId(1L);
    }

    private AuditEvent capturedWrite() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(store).write(captor.capture());
        return captor.getValue();
    }

    private AuditEvent capturedDetachedWrite() {
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(store).writeDetached(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("transaction policy")
    class TransactionPolicy {

        @Test
        @DisplayName("A change joins the caller's transaction, so it can veto it")
        void aChangeJoinsTheCallersTransaction() {
            User target = TestDataFactory.aUser("target");
            target.setId(2L);

            auditService.roleChanged(actor, target, UserRole.USER, UserRole.ADMIN);

            AuditEvent event = capturedWrite();
            assertThat(event.getEventType()).isEqualTo(AuditEventType.ROLE_CHANGED);
            assertThat(event.getActorId()).isEqualTo(1L);
            assertThat(event.getActorName()).isEqualTo("actor");
            assertThat(event.getTargetId()).isEqualTo(2L);
            assertThat(event.getDetail()).isEqualTo("USER -> ADMIN");
            verify(store, never()).writeDetached(any());
        }

        @Test
        @DisplayName("An attempt is written detached, so a refusal cannot roll it back")
        void anAttemptIsWrittenDetached() {
            auditService.loginFailed("guessy");

            AuditEvent event = capturedDetachedWrite();
            assertThat(event.getEventType()).isEqualTo(AuditEventType.LOGIN_FAILED);
            assertThat(event.getActorId()).isNull();
            assertThat(event.getActorName()).isEqualTo("guessy");
            verify(store, never()).write(any());
        }

        @Test
        @DisplayName("A store failure on a change is raised, because the change must fail with it")
        void aStoreFailureOnAChangeIsRaised() {
            RuntimeException failure = new RuntimeException("the trail is unwritable");
            doThrow(failure).when(store).write(any());

            assertThatThrownBy(() -> auditService.whitelistRevoked(actor, "gone@example.com"))
                    .isSameAs(failure);
        }

        @Test
        @DisplayName("A store failure on an attempt is swallowed, because a login must still succeed")
        void aStoreFailureOnAnAttemptIsSwallowed() {
            doThrow(new RuntimeException("the trail is unwritable")).when(store).writeDetached(any());

            auditService.loginSucceeded(actor, "password");

            verify(store).writeDetached(any());
        }
    }

    @Nested
    @DisplayName("recent")
    class Recent {

        private Pageable capturedPage() {
            ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findByOrderByIdDesc(page.capture());
            return page.getValue();
        }

        @Test
        @DisplayName("Asks for the newest rows, with the default page size")
        void asksForTheNewestRows() {
            when(repository.findByOrderByIdDesc(any())).thenReturn(List.of());

            auditService.recent(null, null, null);

            assertThat(capturedPage().getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("Clamps a request for more rows than a page may hold")
        void clampsALargeRequest() {
            when(repository.findByOrderByIdDesc(any())).thenReturn(List.of());

            auditService.recent(null, null, 5_000);

            assertThat(capturedPage().getPageSize()).isEqualTo(500);
        }

        @Test
        @DisplayName("Reads a configured cap below 1 as 1, rather than failing the request")
        void readsACapBelowOneAsOne() {
            // Math.clamp refuses min > max, so the cap is normalised where it is
            // read: a mis-set property must not turn the Logs view into a 500.
            AuditService service = new AuditService(store, repository, 0, 100);
            when(repository.findByOrderByIdDesc(any())).thenReturn(List.of());

            service.recent(null, null, null);

            ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
            verify(repository).findByOrderByIdDesc(page.capture());
            assertThat(page.getValue().getPageSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("Filters by type and cursor when both are given")
        void filtersByTypeAndCursor() {
            when(repository.findByEventTypeAndIdLessThanOrderByIdDesc(
                    eq(AuditEventType.LOGIN_SUCCEEDED), eq(42L), any())).thenReturn(List.of());

            auditService.recent(AuditEventType.LOGIN_SUCCEEDED, 42L, 10);

            verify(repository).findByEventTypeAndIdLessThanOrderByIdDesc(
                    eq(AuditEventType.LOGIN_SUCCEEDED), eq(42L), any());
        }

        @Test
        @DisplayName("Maps a row to the DTO the Logs view reads")
        void mapsRowsToDtos() {
            AuditEvent event = AuditEvent.anonymous(AuditEventType.LOGIN_FAILED, "guessy", null,
                    "wrong password or unknown username", "203.0.113.7");
            event.setId(9L);
            event.setCreatedAt(LocalDateTime.of(2026, 10, 26, 12, 0));
            when(repository.findByOrderByIdDesc(any())).thenReturn(List.of(event));

            List<AuditEventDTO> rows = auditService.recent(null, null, null);

            assertThat(rows).hasSize(1);
            AuditEventDTO dto = rows.getFirst();
            assertThat(dto.id()).isEqualTo(9L);
            assertThat(dto.type()).isEqualTo("LOGIN_FAILED");
            assertThat(dto.actorName()).isEqualTo("guessy");
            assertThat(dto.actorId()).isNull();
            assertThat(dto.occurredAt()).isEqualTo(LocalDateTime.of(2026, 10, 26, 12, 0));
            assertThat(dto.clientIp()).isEqualTo("203.0.113.7");
        }
    }

    @Nested
    @DisplayName("whitelist detail wording")
    class WhitelistDetail {

        /**
         * The trail names the change the request asked for, so a role toggle must
         * not read as a re-admission: that claim is what the request's active flag
         * is for.
         */
        private String detailFor(UserRole role, Boolean active) {
            auditService.whitelistUpdated(actor, "someone@example.com", role, active);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(store, atLeastOnce()).write(captor.capture());
            return captor.getAllValues().getLast().getDetail();
        }

        @Test
        @DisplayName("Names the role the request asked for")
        void namesTheRequestedRole() {
            assertThat(detailFor(UserRole.ADMIN, null)).isEqualTo("role ADMIN");
        }

        @Test
        @DisplayName("Says re-admitted only when the request re-admitted")
        void saysReAdmittedOnlyForAReAdmission() {
            assertThat(detailFor(null, true)).isEqualTo("re-admitted");
            assertThat(detailFor(UserRole.ADMIN, true)).isEqualTo("role ADMIN, re-admitted");
        }

        @Test
        @DisplayName("Says revoked when the request revoked")
        void saysRevokedWhenTheRequestRevoked() {
            assertThat(detailFor(null, false)).isEqualTo("revoked");
            assertThat(detailFor(UserRole.ADMIN, false)).isEqualTo("role ADMIN, revoked");
        }
    }

    @Nested
    @DisplayName("parseType")
    class ParseType {

        @Test
        @DisplayName("Accepts any event type, in any case")
        void acceptsAnyCase() {
            assertThat(AuditService.parseType("login_succeeded")).isEqualTo(AuditEventType.LOGIN_SUCCEEDED);
            assertThat(AuditService.parseType(" ROLE_CHANGED ")).isEqualTo(AuditEventType.ROLE_CHANGED);
        }

        @Test
        @DisplayName("Rejects a missing or unknown type, naming the valid ones")
        void rejectsUnknownTypes() {
            assertThatThrownBy(() -> AuditService.parseType(null))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("ACCOUNT_CREATED");

            assertThatThrownBy(() -> AuditService.parseType("LOGGED_IN"))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("LOGGED_IN");
        }
    }
}
