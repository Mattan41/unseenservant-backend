
package org.kruskopf.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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
 * in their own integration test classes (e.g. UserControllerIT, AdminControllerIT,
 * MessageControllerIT). The blanket rule — no /api endpoint answers an anonymous
 * caller — lives in EndpointAuthenticationIT.
 */
@AutoConfigureMockMvc
@DisplayName("Security Configuration Tests")
class SecurityConfigIT extends AbstractIntegrationTest {

    private final MockMvc mockMvc;
    private final TestRestTemplate restTemplate;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    SecurityConfigIT(MockMvc mockMvc, TestRestTemplate restTemplate) {
        this.mockMvc = mockMvc;
        this.restTemplate = restTemplate;
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

        @Test
        @DisplayName("Auth endpoints are publicly accessible")
        void authEndpointsArePublic() throws Exception {
            mockMvc.perform(get("/api/auth/status"))
                    .andDo(print())
                    .andExpect(status().isNotFound()); // 404 = passed security, no controller
        }
    }

    /**
     * The error page has to stay reachable anonymously.
     * <p>
     * A permitted path with nothing behind it — a missing upload, an unknown URL
     * — makes the servlet container <em>re-dispatch</em> to `/error`, and that
     * dispatch runs through this filter chain a second time. If `/error` is not
     * permitted, the second pass answers 401 and the real status (404, or a 500
     * from a controller) is masked: a missing image reads as a rejected token,
     * and the browser silently falls back to its placeholder image instead of
     * showing that the file is gone.
     * <p>
     * Asserted over real HTTP rather than MockMvc, which never performs the
     * container's error dispatch and so reports the status the mapping produced —
     * it cannot see this at all.
     */
    @Nested
    @DisplayName("Error dispatch stays anonymous")
    class ErrorDispatchStaysAnonymous {

        @Test
        @DisplayName("A missing image answers 404, not 401")
        void missingImageAnswers404() {
            ResponseEntity<String> response =
                    restTemplate.getForEntity("/images/does-not-exist.png", String.class);

            assertThat(response.getStatusCode())
                    .as("a missing upload must not look like a rejected token")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("A permitted path with no handler answers 404, not 401")
        void permittedPathWithoutHandlerAnswers404() {
            ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

            assertThat(response.getStatusCode())
                    .as("the mock-MVC test above cannot see this: only the container's error dispatch can")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("An unknown path is still refused before any handler (denyAll)")
        void unknownPathStaysRefused() {
            ResponseEntity<String> response =
                    restTemplate.getForEntity("/no-such-page", String.class);

            assertThat(response.getStatusCode())
                    .as("nothing unnamed is reachable; only permitted paths surface their own status")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("The /api gate is untouched: an anonymous caller still gets 401")
        void apiGateStillRefusesAnonymousCallers() {
            ResponseEntity<String> response =
                    restTemplate.getForEntity("/api/campaigns", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

}