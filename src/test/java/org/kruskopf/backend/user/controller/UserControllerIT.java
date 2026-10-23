package org.kruskopf.backend.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.auth.JwtService;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the profile endpoints.
 * <p>
 * The PATCH endpoint is the narrow, self-service one: it takes
 * {@code UserProfileUpdateDTO} (display name only) and must refuse the fields
 * that used to be writable through it — the email is the OAuth-linking and
 * whitelist key, and the username is the form-login identity.
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("UserController Integration Tests")
class UserControllerIT extends AbstractIntegrationTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    UserControllerIT(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            JwtService jwtService) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    private User member;
    private User other;
    private User admin;
    private String memberToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        member = TestDataFactory.aUser("profile-member");
        member.setEmail("profile-member@example.com");
        member = userRepository.save(member);

        other = TestDataFactory.aUser("profile-other");
        other.setEmail("profile-other@example.com");
        other = userRepository.save(other);

        admin = TestDataFactory.aUser("profile-admin");
        admin.setEmail("profile-admin@example.com");
        admin.setRole(UserRole.ADMIN);
        admin = userRepository.save(admin);

        memberToken = jwtService.generateToken(member);
        adminToken = jwtService.generateToken(admin);
    }

    private String body(Map<String, Object> payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    private ResultActions patchMe(String token, Map<String, Object> payload) throws Exception {
        return mockMvc.perform(patch("/api/users/" + member.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(payload)));
    }

    @Nested
    @DisplayName("PATCH /api/users/{id}")
    class UpdateProfile {

        @Test
        @DisplayName("Updates the caller's own display name")
        void updatesOwnDisplayName() throws Exception {
            patchMe(memberToken, Map.of("displayName", "New Name"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(member.getId().intValue())))
                    .andExpect(jsonPath("$.displayName", is("New Name")))
                    .andExpect(jsonPath("$.role", is("USER")));

            assertThat(userRepository.findById(member.getId()).orElseThrow().getDisplayName())
                    .isEqualTo("New Name");
        }

        @Test
        @DisplayName("Trims surrounding whitespace")
        void trimsSurroundingWhitespace() throws Exception {
            patchMe(memberToken, Map.of("displayName", "  Padded Name  "))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName", is("Padded Name")));
        }

        @Test
        @DisplayName("Rejects a blank display name")
        void rejectsABlankDisplayName() throws Exception {
            patchMe(memberToken, Map.of("displayName", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.field", is("displayName")));
        }

        @Test
        @DisplayName("Rejects a display name longer than the column allows")
        void rejectsATooLongDisplayName() throws Exception {
            patchMe(memberToken, Map.of("displayName", "x".repeat(51)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail", is("A display name may be at most 50 characters")))
                    .andExpect(jsonPath("$.field", is("displayName")));
        }

        @Test
        @DisplayName("Reports a display name another account already uses as a conflict")
        void reportsADuplicateDisplayName() throws Exception {
            patchMe(memberToken, Map.of("displayName", other.getDisplayName()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.field", is("displayName")));

            assertThat(userRepository.findById(member.getId()).orElseThrow().getDisplayName())
                    .isEqualTo("profile-member");
        }

        @Test
        @DisplayName("Refuses an email change instead of ignoring it")
        void refusesAnEmailChange() throws Exception {
            patchMe(memberToken, Map.of("displayName", "New Name", "email", "brand-new@example.com"))
                    .andExpect(status().isBadRequest());

            // Neither field may have moved: this endpoint is display-name only.
            User reloaded = userRepository.findById(member.getId()).orElseThrow();
            assertThat(reloaded.getEmail()).isEqualTo("profile-member@example.com");
            assertThat(reloaded.getDisplayName()).isEqualTo("profile-member");
        }

        @Test
        @DisplayName("Refuses a username change instead of ignoring it")
        void refusesAUsernameChange() throws Exception {
            patchMe(memberToken, Map.of("displayName", "New Name", "userName", "brand-new"))
                    .andExpect(status().isBadRequest());

            assertThat(userRepository.findById(member.getId()).orElseThrow().getUserName())
                    .isEqualTo("profile-member");
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/{id} authorization")
    class UpdateProfileAuthorization {

        @Test
        @DisplayName("Requires authentication")
        void requiresAuthentication() throws Exception {
            mockMvc.perform(patch("/api/users/" + member.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("displayName", "Nope"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Refuses updating somebody else's profile")
        void refusesUpdatingSomebodyElse() throws Exception {
            mockMvc.perform(patch("/api/users/" + other.getId())
                            .header("Authorization", "Bearer " + memberToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("displayName", "Hijacked"))))
                    .andExpect(status().isForbidden());

            assertThat(userRepository.findById(other.getId()).orElseThrow().getDisplayName())
                    .isEqualTo("profile-other");
        }

        @Test
        @DisplayName("Lets an administrator update somebody else's profile")
        void letsAnAdministratorUpdateSomebodyElse() throws Exception {
            mockMvc.perform(patch("/api/users/" + other.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("displayName", "Renamed By Admin"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName", is("Renamed By Admin")));
        }

        @Test
        @DisplayName("Reports an unknown account as not found")
        void reportsAnUnknownAccount() throws Exception {
            mockMvc.perform(patch("/api/users/999999")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("displayName", "Nowhere"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Profile reads")
    class ProfileReads {

        @Test
        @DisplayName("Returns the caller's own profile")
        void returnsOwnProfile() throws Exception {
            mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + memberToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(member.getId().intValue())))
                    .andExpect(jsonPath("$.role", is("USER")));
        }

        @Test
        @DisplayName("Search excludes the caller")
        void searchExcludesTheCaller() throws Exception {
            mockMvc.perform(get("/api/users/search")
                            .param("query", "profile")
                            .header("Authorization", "Bearer " + memberToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].username", hasItem("profile-other")))
                    .andExpect(jsonPath("$[*].username", not(hasItem("profile-member"))));
        }
    }
}