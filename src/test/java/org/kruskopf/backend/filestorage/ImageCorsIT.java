package org.kruskopf.backend.filestorage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * `/images/**` is permitted without a token, and only one layer may add the CORS
 * header to it: the Spring Security filter ({@code SecurityConfig} registers a
 * {@code CorsConfigurationSource} for {@code /**}). The MVC resource handler used
 * to have its own mapping too; this test is the evidence that removing it changed
 * nothing — an image request must still carry exactly one
 * {@code Access-Control-Allow-Origin}, because a duplicated header is rejected by
 * browsers.
 */
@AutoConfigureMockMvc
@DisplayName("Image CORS headers")
class ImageCorsIT extends AbstractIntegrationTest {

    private final MockMvc mockMvc;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    ImageCorsIT(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("An image request carries exactly one Access-Control-Allow-Origin header")
    void imageRequestCarriesOneAllowOriginHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/images/does-not-exist.png")
                        .header("Origin", "http://localhost:5173"))
                .andReturn();

        List<String> allowOrigin = result.getResponse().getHeaders("Access-Control-Allow-Origin");

        assertThat(allowOrigin)
                .as("browsers reject a response with a duplicated Access-Control-Allow-Origin")
                .hasSize(1);
        assertThat(allowOrigin.get(0)).isEqualTo("http://localhost:5173");
    }
}