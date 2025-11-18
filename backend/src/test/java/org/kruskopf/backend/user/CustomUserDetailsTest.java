package org.kruskopf.backend.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CustomUserDetails.
 * <p>
 * Tests the UserDetails implementation without Spring context.
 */
@DisplayName("CustomUserDetails Unit Tests")
class CustomUserDetailsTest {

    @Test
    @DisplayName("Should correctly map User to UserDetails")
    void shouldMapUserToUserDetails() {
        // Arrange
        User user = new User(
                "google-123",
                ProviderType.GOOGLE,
                "test@example.com",
                "Test User",
                "testuser",
                UserRole.USER,
                "password123"
        );

        // Act
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Assert
        assertThat(userDetails.getUsername()).isEqualTo("test@example.com");
        assertThat(userDetails.getPassword()).isNotNull();
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should return correct authorities for USER role")
    void shouldReturnCorrectAuthoritiesForUser() {
        // Arrange
        User user = new User(
                "google-123",
                ProviderType.GOOGLE,
                "user@example.com",
                "User",
                "user",
                UserRole.USER,
                "password"
        );

        // Act
        CustomUserDetails userDetails = new CustomUserDetails(user);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // Assert
        assertThat(authorities).hasSize(1);
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Should return correct authorities for ADMIN role")
    void shouldReturnCorrectAuthoritiesForAdmin() {
        // Arrange
        User adminUser = new User(
                "google-admin",
                ProviderType.GOOGLE,
                "admin@example.com",
                "Admin",
                "admin",
                UserRole.ADMIN,
                "password"
        );

        // Act
        CustomUserDetails userDetails = new CustomUserDetails(adminUser);
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // Assert
        assertThat(authorities).hasSize(1);
        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should expose underlying User object")
    void shouldExposeUnderlyingUser() {
        // Arrange
        User user = new User(
                "github-456",
                ProviderType.GITHUB,
                "github@example.com",
                "GitHub User",
                "githubuser",
                UserRole.USER,
                "password"
        );

        // Act
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Assert
        assertThat(userDetails.user()).isEqualTo(user);
        assertThat(userDetails.user().getProviderId()).isEqualTo("github-456");
        assertThat(userDetails.user().getProviderType()).isEqualTo(ProviderType.GITHUB);
    }

    @Test
    @DisplayName("Username should map to User email")
    void usernameShouldMapToEmail() {
        // Arrange
        User user = new User(
                "provider-123",
                ProviderType.GOOGLE,
                "myemail@test.com",
                "Name",
                "username123",
                UserRole.USER,
                "password"
        );

        // Act
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Assert
        assertThat(userDetails.getUsername()).isEqualTo("myemail@test.com");
        assertThat(userDetails.getUsername()).isNotEqualTo("username123"); // Username != userName field!
    }
}