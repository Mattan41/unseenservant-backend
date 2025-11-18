
package org.kruskopf.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SecurityConfig.
 * <p>
 * Tests ONLY Spring Security authentication and authorization RULES.
 * <p>
 * NOTE: This test does NOT validate controller logic that depends on CustomUserDetails.
 * Controllers using @AuthenticationPrincipal CustomUserDetails are tested separately
 * in their own integration test classes (e.g., PlayerCharacterControllerIntegrationTest).
 */
@AutoConfigureMockMvc
@DisplayName("Security Configuration Tests")
class SecurityConfigIT extends AbstractIntegrationTest {

    private final MockMvc mockMvc;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    SecurityConfigIT(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Nested
    @DisplayName("Authentication Requirements")
    class AuthenticationRequirements {

        @Test
        @DisplayName("Unauthenticated users get 401 for /api/campaigns")
        void unauthenticatedCannotAccessCampaigns() throws Exception {
            mockMvc.perform(get("/api/campaigns"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated users get 401 for /api/characters")
        void unauthenticatedCannotAccessCharacters() throws Exception {
            mockMvc.perform(get("/api/characters"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated users get 401 for /api/messages")
        void unauthenticatedCannotAccessMessages() throws Exception {
            mockMvc.perform(get("/api/messages"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("/api/auth/me requires authentication")
        void authMeRequiresAuthentication() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Authenticated User Access (Security Rules Only)")
    class AuthenticatedAccess {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("USER can pass security check for /api/campaigns")
        void userCanPassSecurityForCampaigns() throws Exception {
            // This tests that Spring Security ALLOWS the request.
            // The controller might still fail due to business logic or missing data.
            mockMvc.perform(get("/api/campaigns"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        // NOTE: Removed tests for /api/characters and /api/auth/me
        // These endpoints use @AuthenticationPrincipal CustomUserDetails in controllers,
        // which @WithMockUser cannot provide. These will be tested in:
        // - PlayerCharacterControllerIntegrationTest
        // - AuthControllerIntegrationTest (already done!)
    }

    @Nested
    @DisplayName("Role Hierarchy")
    class RoleHierarchy {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN can pass security for USER endpoints")
        void adminCanPassSecurityForUserEndpoints() throws Exception {
            mockMvc.perform(get("/api/campaigns"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("USER cannot access admin-only endpoints")
        void userCannotAccessAdminEndpoints() throws Exception {
            mockMvc.perform(get("/api/admin"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Public Endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("Root path / returns 404 (no controller)")
        void rootPathReturns404() throws Exception {
            mockMvc.perform(get("/"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("OAuth2 authorization endpoint redirects")
        void oauth2AuthorizationRedirects() throws Exception {
            mockMvc.perform(get("/oauth2/authorization/google"))
                    .andDo(print())
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("CSRF Protection")
    class CsrfProtection {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET requests do not require CSRF token")
        void getRequestsDoNotRequireCsrfToken() throws Exception {
            mockMvc.perform(get("/api/campaigns"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }
}