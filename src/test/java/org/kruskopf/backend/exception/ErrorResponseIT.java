package org.kruskopf.backend.exception;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.auth.JwtService;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;
import org.kruskopf.backend.playercharacter.repository.GameCharacterRepository;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error bodies the application itself does not produce: failures raised by the
 * framework or the servlet container before a controller method runs.
 * <p>
 * The point of these tests is the shape, not the status alone: a malformed request
 * body and an unknown path must carry the same RFC 9457 members as a domain
 * failure ({@code type}, {@code title}, {@code status}, {@code detail},
 * {@code instance}), because the client reads {@code detail} and nothing else.
 * <p>
 * The oversize upload is tested at both layers on purpose. A 6MB file passes the
 * servlet's 8MB multipart limit and is refused by {@code FileStorageService}
 * (413 from the advice); a file past 8MB never reaches the application and is
 * refused by the container (413 from {@code ResponseEntityExceptionHandler}).
 * The second one has to go over the real port: MockMvc builds the multipart
 * request in memory and never runs the container's parser, so it cannot
 * reproduce the limit.
 */
@AutoConfigureMockMvc
@AutoConfigureTestRestTemplate
@TestPropertySource(properties = "server.tomcat.max-swallow-size=-1")
@DisplayName("Error response Integration Tests")
class ErrorResponseIT extends AbstractIntegrationTest {

    private static final int SIX_MEGABYTES = 6 * 1024 * 1024;
    private static final int PAST_THE_SERVLET_LIMIT = 8 * 1024 * 1024 + 1;

    private final MockMvc mockMvc;
    private final TestRestTemplate restTemplate;
    private final UserRepository userRepository;
    private final GameCharacterRepository characterRepository;
    private final JwtService jwtService;

    private User owner;
    private Long characterId;
    private String token;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    ErrorResponseIT(
            MockMvc mockMvc,
            TestRestTemplate restTemplate,
            UserRepository userRepository,
            GameCharacterRepository characterRepository,
            JwtService jwtService) {
        this.mockMvc = mockMvc;
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.characterRepository = characterRepository;
        this.jwtService = jwtService;
    }

    @BeforeEach
    void setUp() {
        // Committed on purpose: the real-port request runs on another connection
        // and cannot see a transaction this test would still be holding open.
        owner = userRepository.save(TestDataFactory.aUser("error.response.owner"));
        GameCharacter character = characterRepository.save(TestDataFactory.aCharacter(owner));
        characterId = character.getId();
        token = jwtService.generateToken(owner);
    }

    @AfterEach
    void tearDown() {
        characterRepository.deleteById(characterId);
        userRepository.deleteById(owner.getId());
    }

    @Test
    @DisplayName("Answers a malformed request body with a ProblemDetail 400")
    void answersMalformedBodyWithProblemDetail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("Answers an unknown path with a ProblemDetail 404")
    void answersUnknownPathWithProblemDetail() throws Exception {
        mockMvc.perform(get("/api/auth/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("Answers an upload past the application limit with a ProblemDetail 413")
    void answersApplicationUploadLimitWithProblemDetail() throws Exception {
        mockMvc.perform(multipart("/api/characters/" + characterId + "/image")
                        .file(new MockMultipartFile("file", "too-large.png", "image/png",
                                new byte[SIX_MEGABYTES]))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isContentTooLarge())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(413)))
                .andExpect(jsonPath("$.detail", is("File size exceeds maximum limit of 5MB")));
    }

    @Test
    @DisplayName("Answers an upload past the servlet limit with a ProblemDetail 413 over the real port")
    void answersServletUploadLimitWithProblemDetail() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(new byte[PAST_THE_SERVLET_LIMIT]) {
            @Override
            public String getFilename() {
                return "past-the-servlet-limit.png";
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/characters/" + characterId + "/image",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        // Compared by code: the container reports 413 as HttpStatus.CONTENT_TOO_LARGE
        // (RFC 9110), while HttpStatus.PAYLOAD_TOO_LARGE is the same 413 under the
        // name Framework 7 deprecated. Enum identity would fail on the rename.
        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody())
                .as("a 413 from the container must still carry a detail")
                .contains("\"detail\"");
    }
}
