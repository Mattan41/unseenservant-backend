package org.kruskopf.backend.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The login endpoint over HTTP for the credentials half of the contract: 401 with
 * one shared body for both a wrong password and an unknown account, 200 with the
 * token in the response header otherwise. See docs/INTEGRATION_CONTRACT.md.
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("Login credentials Integration Tests")
class LoginCredentialsIT extends AbstractIntegrationTest {

    private static final String USERNAME = "user1@test.com";
    private static final String PASSWORD = "password123";

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    LoginCredentialsIT(MockMvc mockMvc, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Answers 401 for a wrong password and for an unknown account, with the same body")
    void refusesBothTheSameWay() throws Exception {
        mockMvc.perform(login("nobody@test.com", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid username or password"));

        saveUser();

        mockMvc.perform(login(USERNAME, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid username or password"));
    }

    @Test
    @DisplayName("Answers 200 with the token in the Authorization header")
    void acceptsTheRightPassword() throws Exception {
        saveUser();

        mockMvc.perform(login(USERNAME, PASSWORD))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", startsWith("Bearer ")))
                .andExpect(jsonPath("$.username", is(USERNAME)));
    }

    private void saveUser() {
        User user = TestDataFactory.aUser("user1");
        user.setEmail(USERNAME);
        user.setUserName(USERNAME);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        userRepository.save(user);
    }

    private static MockHttpServletRequestBuilder login(String username, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }
}