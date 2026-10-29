package org.kruskopf.backend.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.audit.AuditService;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.kruskopf.backend.user.service.UserService;
import org.kruskopf.backend.whitelist.EmailWhitelist;
import org.kruskopf.backend.whitelist.EmailWhitelistService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Unit Tests")
class AdminServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailWhitelistService emailWhitelistService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminService adminService;

    private static final long ACTING_ADMIN_ID = 1L;
    private static final long TARGET_ID = 2L;

    /** The administrator making the changes; an id is what the self-check compares. */
    private User actor;

    @BeforeEach
    void setUp() {
        actor = TestDataFactory.aUser("actor");
        actor.setId(ACTING_ADMIN_ID);
    }

    @Nested
    @DisplayName("changeUserRole")
    class ChangeUserRole {

        @Test
        @DisplayName("Refuses to change the caller's own role")
        void refusesSelfChange() {
            assertThatThrownBy(
                    () -> adminService.changeUserRole(ACTING_ADMIN_ID, UserRole.USER, actor))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("your own role");

            verifyNoInteractions(userRepository);
            // A mis-click that changes nothing is deliberately not part of the
            // trail: it is answered before the lookup, and the administrator who
            // tried it is told.
            verifyNoInteractions(auditService);
        }

        @Test
        @DisplayName("Refuses to demote the last administrator, and records the refusal")
        void refusesDemotingTheLastAdministrator() {
            User target = mock(User.class);
            when(target.getRole()).thenReturn(UserRole.ADMIN);
            when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
            when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

            assertThatThrownBy(() -> adminService.changeUserRole(TARGET_ID, UserRole.USER, actor))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("last administrator");

            verify(userService, never()).updateRole(anyLong(), any());
            verify(auditService).roleChangeRefused(actor, target, "the last administrator cannot be demoted");
        }

        @Test
        @DisplayName("Demotes an administrator while another one remains")
        void demotesWhenAnotherAdminRemains() {
            User target = mock(User.class);
            when(target.getRole()).thenReturn(UserRole.ADMIN);
            when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
            when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(2L);

            User updated = mock(User.class);
            when(updated.getEmail()).thenReturn("target@example.com");
            when(userService.updateRole(TARGET_ID, UserRole.USER)).thenReturn(updated);
            UserDTO dto = new UserDTO(TARGET_ID, "target", "target@example.com", "Target", "USER");
            when(userService.toDTO(updated)).thenReturn(dto);

            assertThat(adminService.changeUserRole(TARGET_ID, UserRole.USER, actor)).isEqualTo(dto);
            verify(emailWhitelistService).syncRoleForEmail("target@example.com", UserRole.USER);
            // The trail names the role it came from, not only the one it went to.
            verify(auditService).roleChanged(actor, updated, UserRole.ADMIN, UserRole.USER);
        }

        @Test
        @DisplayName("Promotes a user and mirrors the address's whitelist row")
        void promotesAndMirrorsTheWhitelistRow() {
            User target = mock(User.class);
            when(target.getRole()).thenReturn(UserRole.USER);
            when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

            User updated = mock(User.class);
            when(updated.getEmail()).thenReturn("target@example.com");
            when(userService.updateRole(TARGET_ID, UserRole.ADMIN)).thenReturn(updated);
            UserDTO dto = new UserDTO(TARGET_ID, "target", "target@example.com", "Target", "ADMIN");
            when(userService.toDTO(updated)).thenReturn(dto);

            assertThat(adminService.changeUserRole(TARGET_ID, UserRole.ADMIN, actor)).isEqualTo(dto);
            verify(emailWhitelistService).syncRoleForEmail("target@example.com", UserRole.ADMIN);
            // Promotion never consults the administrator count.
            verify(userRepository, never()).countByRole(any());
            verify(auditService).roleChanged(actor, updated, UserRole.USER, UserRole.ADMIN);
        }

        @Test
        @DisplayName("Reports a missing account as not found")
        void reportsMissingTarget() {
            when(userRepository.findById(TARGET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.changeUserRole(TARGET_ID, UserRole.ADMIN, actor))
                    .isInstanceOf(ResourceNotFoundException.class);

            // Nothing was refused by a rule, so nothing is reported as a refusal.
            verifyNoInteractions(auditService);
        }
    }

    @Nested
    @DisplayName("whitelist")
    class Whitelist {

        @Test
        @DisplayName("Requires an email address when adding")
        void requiresAnEmail() {
            assertThatThrownBy(() -> adminService.addWhitelist(actor, "  ", "USER"))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("email address");
        }

        @Test
        @DisplayName("Rejects a string that is not an address")
        void rejectsANonAddress() {
            assertThatThrownBy(() -> adminService.addWhitelist(actor, "not-an-address", "USER"))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("email address");
        }

        @Test
        @DisplayName("Adds the address through the whitelist service, and records it")
        void addsThroughTheWhitelistService() {
            EmailWhitelist saved = new EmailWhitelist("new@example.com", UserRole.USER);
            saved.setId(7L);
            when(emailWhitelistService.addOrReactivateEmail("New@Example.com", UserRole.USER))
                    .thenReturn(saved);

            assertThat(adminService.addWhitelist(actor, "New@Example.com", "user").email())
                    .isEqualTo("new@example.com");
            // The trail names the address as stored, not as typed.
            verify(auditService).whitelistAdded(actor, "new@example.com", UserRole.USER);
        }

        @Test
        @DisplayName("Requires something to update")
        void requiresSomethingToUpdate() {
            assertThatThrownBy(() -> adminService.updateWhitelist(actor, 5L, null, null))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Nothing to update");
        }

        @Test
        @DisplayName("Records the row's state after an update, not the request")
        void recordsTheStateAfterAnUpdate() {
            EmailWhitelist entry = new EmailWhitelist("promote-me@example.com", UserRole.ADMIN);
            entry.setId(5L);
            when(emailWhitelistService.updateEmail(5L, UserRole.ADMIN, null)).thenReturn(entry);

            adminService.updateWhitelist(actor, 5L, "ADMIN", null);

            verify(auditService).whitelistUpdated(actor, "promote-me@example.com", UserRole.ADMIN, true);
        }

        @Test
        @DisplayName("Revokes an address by id, and records it")
        void revokesById() {
            EmailWhitelist entry = new EmailWhitelist("revoke-me@example.com", UserRole.USER);
            entry.setId(5L);
            entry.setActive(false);
            when(emailWhitelistService.removeEmailById(5L)).thenReturn(entry);

            adminService.removeWhitelist(actor, 5L);

            verify(emailWhitelistService).removeEmailById(5L);
            verify(auditService).whitelistRevoked(actor, "revoke-me@example.com");
        }
    }

    @Nested
    @DisplayName("parseRole")
    class ParseRole {

        @Test
        @DisplayName("Accepts either role, in any case")
        void acceptsBothRoles() {
            assertThat(AdminService.parseRole("admin")).isEqualTo(UserRole.ADMIN);
            assertThat(AdminService.parseRole(" ADMIN ")).isEqualTo(UserRole.ADMIN);
            assertThat(AdminService.parseRole("user")).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("Rejects a missing or unknown role, naming the allowed ones")
        void rejectsUnknownRoles() {
            assertThatThrownBy(() -> AdminService.parseRole(null))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("USER");

            assertThatThrownBy(() -> AdminService.parseRole("superuser"))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("superuser");
        }
    }
}