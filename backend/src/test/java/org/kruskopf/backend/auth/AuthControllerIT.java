package org.kruskopf.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.user.CustomUserDetails;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController endpoints.
 * <p>
 * Tests the /api/auth/me endpoint with different authentication scenarios:
 * - Unauthenticated requests
 * - Authenticated users with different roles
 * - OAuth2 authentication simulation
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("AuthController Integration Tests")
class AuthControllerIT extends AbstractIntegrationTest {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    AuthControllerIT(MockMvc mockMvc, UserRepository userRepository) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetCurrentUser {

        @Test
        @DisplayName("Should return 401 Unauthorized when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 200 OK with user data when authenticated")
        void shouldReturn200WithUserDataWhenAuthenticated() throws Exception {
            // Arrange: Create a test user in database
            User testUser = new User(
                    "google-123456",
                    ProviderType.GOOGLE,
                    "user1@test.com",
                    "Test User",
                    "user1@test.com",
                    UserRole.USER,
                    "password"
            );
            testUser = userRepository.save(testUser);

            // Create CustomUserDetails for Spring Security
            CustomUserDetails userDetails = new CustomUserDetails(testUser);

            // Act & Assert: Call endpoint with authentication
            mockMvc.perform(get("/api/auth/me")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.id", is(testUser.getId().intValue())))
                    .andExpect(jsonPath("$.username", is("user1@test.com")))
                    .andExpect(jsonPath("$.email", is("user1@test.com")))
                    .andExpect(jsonPath("$.role", is("ROLE_USER")));
        }

        @Test
        @DisplayName("Should return correct role for ADMIN user")
        void shouldReturnCorrectRoleForAdmin() throws Exception {
            // Arrange: Create an admin user
            User adminUser = new User(
                    "google-admin-123",
                    ProviderType.GOOGLE,
                    "admin@test.com",
                    "Admin User",
                    "admin@test.com",
                    UserRole.ADMIN,
                    "password"
            );
            adminUser = userRepository.save(adminUser);

            CustomUserDetails userDetails = new CustomUserDetails(adminUser);

            // Act & Assert
            mockMvc.perform(get("/api/auth/me")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(adminUser.getId().intValue())))
                    .andExpect(jsonPath("$.role", is("ROLE_ADMIN")));
        }

        @Test
        @DisplayName("Should handle GitHub user correctly")
        void shouldHandleGitHubUser() throws Exception {
            // Arrange: Create a GitHub user
            User githubUser = new User(
                    "github-12345",
                    ProviderType.GITHUB,
                    "user2@test.com",
                    "GitHub User",
                    "user2@test.com",
                    UserRole.USER,
                    "password"
            );
            githubUser = userRepository.save(githubUser);

            CustomUserDetails userDetails = new CustomUserDetails(githubUser);

            // Act & Assert
            mockMvc.perform(get("/api/auth/me")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("user2@test.com")));
        }
    }
}