package org.kruskopf.backend.admin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.audit.entity.AuditEvent;
import org.kruskopf.backend.audit.entity.AuditEventType;
import org.kruskopf.backend.audit.repository.AuditEventRepository;
import org.kruskopf.backend.auth.JwtService;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.kruskopf.backend.whitelist.EmailWhitelist;
import org.kruskopf.backend.whitelist.EmailWhitelistRepository;
import org.kruskopf.backend.whitelist.EmailWhitelistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the administrator surface.
 * <p>
 * Covers both halves of the admin-only rule — the `/api/admin/**` matcher in
 * {@code SecurityConfig} and the class-level {@code @PreAuthorize} — plus the
 * domain guards and the whitelist's soft-delete/reactivate behaviour, which are
 * the parts a refactor could quietly break.
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("AdminController Integration Tests")
class AdminControllerIT extends AbstractIntegrationTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final EmailWhitelistRepository whitelistRepository;
    private final EmailWhitelistService emailWhitelistService;
    private final AuditEventRepository auditEventRepository;
    private final JwtService jwtService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    AdminControllerIT(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            EmailWhitelistRepository whitelistRepository,
            EmailWhitelistService emailWhitelistService,
            AuditEventRepository auditEventRepository,
            JwtService jwtService) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.whitelistRepository = whitelistRepository;
        this.emailWhitelistService = emailWhitelistService;
        this.auditEventRepository = auditEventRepository;
        this.jwtService = jwtService;
    }

    private User admin;
    private User member;
    private String adminToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        admin = TestDataFactory.aUser("admin-it");
        admin.setEmail("admin-it@example.com");
        admin.setRole(UserRole.ADMIN);
        admin = userRepository.save(admin);

        member = TestDataFactory.aUser("member-it");
        member.setEmail("member-it@example.com");
        member = userRepository.save(member);

        adminToken = jwtService.generateToken(admin);
        memberToken = jwtService.generateToken(member);
    }

    private String body(Map<String, Object> payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    @Nested
    @DisplayName("GET /api/admin/users")
    class ListUsers {

        @Test
        @DisplayName("Is refused for anonymous callers")
        void isRefusedForAnonymousCallers() throws Exception {
            mockMvc.perform(get("/api/admin/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Is refused for ordinary accounts")
        void isRefusedForOrdinaryAccounts() throws Exception {
            mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + memberToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Lists every account with its role for an administrator")
        void listsEveryAccountForAnAdministrator() throws Exception {
            mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].email", hasItem("admin-it@example.com")))
                    .andExpect(jsonPath("$[*].email", hasItem("member-it@example.com")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/users/{id}/role")
    class ChangeRole {

        @Test
        @DisplayName("Promotes an account and mirrors its whitelist row")
        void promotesAnAccountAndMirrorsItsWhitelistRow() throws Exception {
            whitelistRepository.save(new EmailWhitelist("member-it@example.com", UserRole.USER));

            mockMvc.perform(patch("/api/admin/users/" + member.getId() + "/role")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("role", "ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role", is("ADMIN")));

            assertThat(userRepository.findById(member.getId()).orElseThrow().getRole())
                    .isEqualTo(UserRole.ADMIN);
            assertThat(whitelistRepository.findByEmailIgnoreCase("member-it@example.com")
                    .orElseThrow().getRole()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("Demotes another administrator while one remains")
        void demotesAnotherAdministrator() throws Exception {
            User second = TestDataFactory.aUser("second-admin-it");
            second.setEmail("second-admin-it@example.com");
            second.setRole(UserRole.ADMIN);
            second = userRepository.save(second);

            mockMvc.perform(patch("/api/admin/users/" + second.getId() + "/role")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("role", "USER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role", is("USER")));
        }

        @Test
        @DisplayName("Refuses to change the caller's own role")
        void refusesSelfChange() throws Exception {
            mockMvc.perform(patch("/api/admin/users/" + admin.getId() + "/role")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("role", "USER"))))
                    .andExpect(status().isBadRequest());

            assertThat(userRepository.findById(admin.getId()).orElseThrow().getRole())
                    .isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("Rejects an unknown role")
        void rejectsAnUnknownRole() throws Exception {
            mockMvc.perform(patch("/api/admin/users/" + member.getId() + "/role")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("role", "SUPERUSER"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Reports an unknown account as not found")
        void reportsAnUnknownAccount() throws Exception {
            mockMvc.perform(patch("/api/admin/users/999999/role")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("role", "ADMIN"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Is refused for ordinary accounts")
        void isRefusedForOrdinaryAccounts() throws Exception {
            mockMvc.perform(patch("/api/admin/users/" + member.getId() + "/role")
                            .header("Authorization", "Bearer " + memberToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("role", "ADMIN"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("whitelist")
    class Whitelist {

        @Test
        @DisplayName("Lists the seeded rows for an administrator")
        void listsTheSeededRowsForAnAdministrator() throws Exception {
            mockMvc.perform(get("/api/admin/whitelist").header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].email", hasItem("admin@test.com")));
        }

        @Test
        @DisplayName("Is refused for ordinary accounts")
        void isRefusedForOrdinaryAccounts() throws Exception {
            mockMvc.perform(get("/api/admin/whitelist").header("Authorization", "Bearer " + memberToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admits a new address, storing it lower-cased")
        void admitsANewAddressStoringItLowerCased() throws Exception {
            mockMvc.perform(post("/api/admin/whitelist")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("email", "MixedCase@Example.com", "role", "ADMIN"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email", is("mixedcase@example.com")))
                    .andExpect(jsonPath("$.role", is("ADMIN")))
                    .andExpect(jsonPath("$.active", is(true)));

            // The login lookup is case-insensitive, so stored casing cannot lock anyone out.
            assertThat(emailWhitelistService.isEmailWhitelisted("mixedcase@example.com")).isTrue();
            assertThat(emailWhitelistService.isEmailWhitelisted("MIXEDCASE@EXAMPLE.COM")).isTrue();
        }

        @Test
        @DisplayName("Re-activates a revoked address instead of duplicating it")
        void reactivatesARevokedAddress() throws Exception {
            EmailWhitelist revoked = new EmailWhitelist("revoked@example.com", UserRole.USER);
            revoked.setActive(false);
            revoked = whitelistRepository.save(revoked);
            long before = whitelistRepository.count();

            mockMvc.perform(post("/api/admin/whitelist")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("email", "revoked@example.com", "role", "ADMIN"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(revoked.getId().intValue())))
                    .andExpect(jsonPath("$.active", is(true)))
                    .andExpect(jsonPath("$.role", is("ADMIN")));

            // The email column is unique, so this must update the row, not insert a second one.
            assertThat(whitelistRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("Rejects an address that is not one")
        void rejectsAnAddressThatIsNotOne() throws Exception {
            mockMvc.perform(post("/api/admin/whitelist")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("email", "not-an-address", "role", "USER"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Re-roles an address")
        void reRolesAnAddress() throws Exception {
            EmailWhitelist entry = whitelistRepository.save(
                    new EmailWhitelist("promote-me@example.com", UserRole.USER));

            mockMvc.perform(patch("/api/admin/whitelist/" + entry.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("role", "ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role", is("ADMIN")))
                    .andExpect(jsonPath("$.active", is(true)));
        }

        @Test
        @DisplayName("Revokes an address without deleting the row")
        void revokesAnAddressWithoutDeletingTheRow() throws Exception {
            EmailWhitelist entry = whitelistRepository.save(
                    new EmailWhitelist("revoke-me@example.com", UserRole.USER));
            long before = whitelistRepository.count();

            mockMvc.perform(patch("/api/admin/whitelist/" + entry.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("active", false))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active", is(false)));

            assertThat(whitelistRepository.count()).isEqualTo(before);
            assertThat(emailWhitelistService.isEmailWhitelisted("revoke-me@example.com")).isFalse();
        }

        @Test
        @DisplayName("Rejects an update that changes nothing")
        void rejectsAnUpdateThatChangesNothing() throws Exception {
            EmailWhitelist entry = whitelistRepository.save(
                    new EmailWhitelist("empty-update@example.com", UserRole.USER));

            mockMvc.perform(patch("/api/admin/whitelist/" + entry.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Soft-deletes through DELETE")
        void softDeletesThroughDelete() throws Exception {
            EmailWhitelist entry = whitelistRepository.save(
                    new EmailWhitelist("delete-me@example.com", UserRole.USER));

            mockMvc.perform(delete("/api/admin/whitelist/" + entry.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            assertThat(whitelistRepository.findById(entry.getId()).orElseThrow().isActive()).isFalse();
        }

        @Test
        @DisplayName("Reports an unknown row as not found")
        void reportsAnUnknownRow() throws Exception {
            mockMvc.perform(delete("/api/admin/whitelist/999999")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());

            mockMvc.perform(patch("/api/admin/whitelist/999999")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("active", false))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/logs")
    class Logs {

        /**
         * The trail is a table like any other, but these assertions are about
         * exact contents, so any row another class left behind would be noise.
         * The delete runs inside this test's transaction, so it is rolled back
         * again and cannot disturb anybody else.
         */
        @BeforeEach
        void startFromAnEmptyTrail() {
            auditEventRepository.deleteAll();
        }

        private AuditEvent aFailedLogin(String attempted) {
            return AuditEvent.anonymous(AuditEventType.LOGIN_FAILED, attempted, null,
                    "wrong password or unknown username", "203.0.113.7");
        }

        private AuditEvent aSucceededLogin() {
            return AuditEvent.by(AuditEventType.LOGIN_SUCCEEDED, admin, "password", "203.0.113.7");
        }

        @Test
        @DisplayName("Is refused for anonymous callers")
        void isRefusedForAnonymousCallers() throws Exception {
            mockMvc.perform(get("/api/admin/logs"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Is refused for ordinary accounts")
        void isRefusedForOrdinaryAccounts() throws Exception {
            mockMvc.perform(get("/api/admin/logs").header("Authorization", "Bearer " + memberToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Lists the newest row first, with the fields the view reads")
        void listsTheNewestRowFirst() throws Exception {
            auditEventRepository.save(aFailedLogin("guessy"));
            auditEventRepository.save(aSucceededLogin());

            mockMvc.perform(get("/api/admin/logs").header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].type", is("LOGIN_SUCCEEDED")))
                    .andExpect(jsonPath("$[0].actorId", is(admin.getId().intValue())))
                    .andExpect(jsonPath("$[0].actorName", is("admin-it")))
                    .andExpect(jsonPath("$[0].detail", is("password")))
                    .andExpect(jsonPath("$[0].clientIp", is("203.0.113.7")))
                    .andExpect(jsonPath("$[0].occurredAt").isNotEmpty())
                    // A failed login has no account behind it, and still names
                    // what was attempted.
                    .andExpect(jsonPath("$[1].type", is("LOGIN_FAILED")))
                    .andExpect(jsonPath("$[1].actorId").doesNotExist())
                    .andExpect(jsonPath("$[1].actorName", is("guessy")));
        }

        @Test
        @DisplayName("Filters by event type, whatever the casing of the filter")
        void filtersByEventType() throws Exception {
            auditEventRepository.save(aFailedLogin("guessy"));
            auditEventRepository.save(aSucceededLogin());

            mockMvc.perform(get("/api/admin/logs?type=login_failed")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].type", is("LOGIN_FAILED")));
        }

        @Test
        @DisplayName("Names the valid types when the filter is unknown")
        void namesTheValidTypesForAnUnknownFilter() throws Exception {
            mockMvc.perform(get("/api/admin/logs?type=LOGGED_IN")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail", containsString("ACCOUNT_CREATED")));
        }

        @Test
        @DisplayName("Pages with the id of the last row of the previous page")
        void pagesWithTheCursor() throws Exception {
            long first = auditEventRepository.save(aFailedLogin("first")).getId();
            long last = auditEventRepository.save(aSucceededLogin()).getId();

            mockMvc.perform(get("/api/admin/logs?beforeId=" + last)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is((int) first)));
        }

        @Test
        @DisplayName("Honours a limit")
        void honoursALimit() throws Exception {
            auditEventRepository.save(aFailedLogin("one"));
            auditEventRepository.save(aFailedLogin("two"));
            auditEventRepository.save(aSucceededLogin());

            mockMvc.perform(get("/api/admin/logs?limit=2")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }
}