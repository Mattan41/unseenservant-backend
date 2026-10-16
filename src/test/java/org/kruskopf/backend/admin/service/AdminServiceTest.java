package org.kruskopf.backend.admin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
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

    @InjectMocks
    private AdminService adminService;

    private static final long ACTING_ADMIN_ID = 1L;
    private static final long TARGET_ID = 2L;

    @Nested
    @DisplayName("changeUserRole")
    class ChangeUserRole {

        @Test
        @DisplayName("Refuses to change the caller's own role")
        void refusesSelfChange() {
            assertThatThrownBy(
                    () -> adminService.changeUserRole(ACTING_ADMIN_ID, UserRole.USER, ACTING_ADMIN_ID))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("your own role");

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Refuses to demote the last administrator")
        void refusesDemotingTheLastAdministrator() {
            User target = mock(User.class);
            when(target.getRole()).thenReturn(UserRole.ADMIN);
            when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
            when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

            assertThatThrownBy(() -> adminService.changeUserRole(TARGET_ID, UserRole.USER, ACTING_ADMIN_ID))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("last administrator");

            verify(userService, never()).updateRole(anyLong(), any());
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

            assertThat(adminService.changeUserRole(TARGET_ID, UserRole.USER, ACTING_ADMIN_ID)).isEqualTo(dto);
            verify(emailWhitelistService).syncRoleForEmail("target@example.com", UserRole.USER);
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

            assertThat(adminService.changeUserRole(TARGET_ID, UserRole.ADMIN, ACTING_ADMIN_ID)).isEqualTo(dto);
            verify(emailWhitelistService).syncRoleForEmail("target@example.com", UserRole.ADMIN);
            // Promotion never consults the administrator count.
            verify(userRepository, never()).countByRole(any());
        }

        @Test
        @DisplayName("Reports a missing account as not found")
        void reportsMissingTarget() {
            when(userRepository.findById(TARGET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.changeUserRole(TARGET_ID, UserRole.ADMIN, ACTING_ADMIN_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("whitelist")
    class Whitelist {

        @Test
        @DisplayName("Requires an email address when adding")
        void requiresAnEmail() {
            assertThatThrownBy(() -> adminService.addWhitelist("  ", "USER"))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("email address");
        }

        @Test
        @DisplayName("Rejects a string that is not an address")
        void rejectsANonAddress() {
            assertThatThrownBy(() -> adminService.addWhitelist("not-an-address", "USER"))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("email address");
        }

        @Test
        @DisplayName("Adds the address through the whitelist service")
        void addsThroughTheWhitelistService() {
            EmailWhitelist saved = new EmailWhitelist("new@example.com", UserRole.USER);
            saved.setId(7L);
            when(emailWhitelistService.addOrReactivateEmail("New@Example.com", UserRole.USER))
                    .thenReturn(saved);

            assertThat(adminService.addWhitelist("New@Example.com", "user").email())
                    .isEqualTo("new@example.com");
        }

        @Test
        @DisplayName("Requires something to update")
        void requiresSomethingToUpdate() {
            assertThatThrownBy(() -> adminService.updateWhitelist(5L, null, null))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Nothing to update");
        }

        @Test
        @DisplayName("Revokes an address by id")
        void revokesById() {
            adminService.removeWhitelist(5L);

            verify(emailWhitelistService).removeEmailById(5L);
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