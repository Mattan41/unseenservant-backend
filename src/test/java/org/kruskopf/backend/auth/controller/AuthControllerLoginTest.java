package org.kruskopf.backend.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.auth.JwtService;
import org.kruskopf.backend.auth.dto.LoginRequest;
import org.kruskopf.backend.ratelimit.LoginAttemptLimiter;
import org.kruskopf.backend.ratelimit.RateLimitProperties;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The login endpoint's credential handling. The throttle is off here because it
 * has its own tests; what this pins is that a guess costs one hash comparison
 * whether or not the account exists - see docs/ARCHITECTURE.md ("Login").
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController login Unit Tests")
class AuthControllerLoginTest {

    private static final String UNKNOWN_USER_HASH = "hash-of-a-random-value";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(false);

        when(passwordEncoder.encode(anyString())).thenReturn(UNKNOWN_USER_HASH);
        authController = new AuthController(
                userRepository, passwordEncoder, jwtService, new LoginAttemptLimiter(properties));
    }

    @Test
    @DisplayName("Compares the password against a stand-in when the account is unknown")
    void comparesAgainstStandInForUnknownAccount() {
        when(userRepository.findByUserName("nobody")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authController.login(login("nobody"), new MockHttpServletRequest()))
                .isInstanceOf(BadCredentialsException.class);

        // The same call a known account gets: that equality is the point, and it
        // is what stops the endpoint from answering "does this user exist?".
        verify(passwordEncoder).matches("password123", UNKNOWN_USER_HASH);
    }

    @Test
    @DisplayName("Compares the password against the stored hash for a known account")
    void comparesStoredHashForKnownAccount() {
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user("user1", "stored-hash")));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authController.login(login("user1"), new MockHttpServletRequest()))
                .isInstanceOf(BadCredentialsException.class);

        verify(passwordEncoder).matches("password123", "stored-hash");
    }

    @Test
    @DisplayName("Answers the AuthDTO for a matching password")
    void answersAuthDtoForMatchingPassword() {
        User user = user("user1", "stored-hash");
        when(userRepository.findByUserName("user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("a-token");

        var response = authController.login(login("user1"), new MockHttpServletRequest());

        verify(passwordEncoder).matches("password123", "stored-hash");
        org.assertj.core.api.Assertions.assertThat(response.getBody()).isNotNull();
    }

    private static LoginRequest login(String username) {
        return new LoginRequest(username, "password123");
    }

    private static User user(String username, String passwordHash) {
        User user = new User();
        user.setUserName(username);
        user.setEmail(username + "@example.com");
        user.setPassword(passwordHash);
        user.setRole(UserRole.USER);
        return user;
    }
}