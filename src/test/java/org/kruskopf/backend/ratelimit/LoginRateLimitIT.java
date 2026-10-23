package org.kruskopf.backend.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The throttle as a client sees it: the attempt after the budget is answered 429
 * with {@code Retry-After}, and a different account still gets through. Budgets
 * are one attempt per account over a long window, and each test uses its own
 * usernames, so nothing here depends on order or on timing.
 * <p>
 * It declares its own container because the shared one is reused between runs -
 * see {@code SchemaValidationIT}, which documents the same hazard.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.window=10m",
        "app.rate-limit.ip-attempts=1000",
        "app.rate-limit.username-attempts=1"
})
@DisplayName("Login throttle Integration Tests")
class LoginRateLimitIT {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.11")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    private final MockMvc mockMvc;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    LoginRateLimitIT(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("Answers the attempt after the budget with 429 and Retry-After")
    void answersWith429AndRetryAfter() throws Exception {
        MvcResult first = mockMvc.perform(login("throttle.probe.one@test.com")).andReturn();

        // What the credentials did is not this test's business: whether the
        // attempt was rejected as wrong or failed some other way, the first one
        // must not be the one that gets throttled.
        assertThat(first.getResponse().getStatus()).isNotEqualTo(429);

        mockMvc.perform(login("throttle.probe.one@test.com"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                // The same body comes back whatever was attempted, so the
                // response cannot be read as a signal about the account.
                .andExpect(content().string(startsWith("Too many attempts. Please try again in")));
    }

    @Test
    @DisplayName("Throttles one account without touching another")
    void throttlesOneAccountOnly() throws Exception {
        mockMvc.perform(login("throttle.probe.two@test.com"));
        mockMvc.perform(login("throttle.probe.two@test.com")).andExpect(status().isTooManyRequests());

        MvcResult other = mockMvc.perform(login("throttle.probe.three@test.com")).andReturn();

        assertThat(other.getResponse().getStatus()).isNotEqualTo(429);
    }

    /** A form login for {@code username}, which is what the demo profile serves. */
    private static MockHttpServletRequestBuilder login(String username) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"password123\"}");
    }
}