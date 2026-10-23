package org.kruskopf.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * The blanket rule: **no `/api` endpoint answers an anonymous caller with
 * anything but 401 or 403**.
 *
 * <p>Rather than listing the endpoints by hand, this walks the request mappings,
 * so an endpoint added tomorrow is covered the moment it is mapped — the failure
 * this exists to catch is exactly "someone added a controller and forgot the
 * gate". It is what would have caught the `/api/admin` matcher that only covered
 * the bare path, and the auth matcher that made `/api/auth/me` public.
 *
 * <p>Only the two login entry points may be reached anonymously:
 * {@code POST /api/auth/login} and {@code GET /api/auth/oauth-init}.
 */
@AutoConfigureMockMvc
@DisplayName("/api endpoints refuse anonymous callers")
class EndpointAuthenticationIT extends AbstractIntegrationTest {

    /** Reached before a caller has a token, by definition. */
    private static final Set<String> PUBLIC = Set.of("POST /api/auth/login", "GET /api/auth/oauth-init");

    private final MockMvc mockMvc;
    private final RequestMappingHandlerMapping handlerMapping;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    EndpointAuthenticationIT(MockMvc mockMvc, RequestMappingHandlerMapping handlerMapping) {
        this.mockMvc = mockMvc;
        this.handlerMapping = handlerMapping;
    }

    @Test
    @DisplayName("Every mapped /api endpoint refuses an anonymous request")
    void everyApiEndpointRefusesAnonymousRequests() throws Exception {
        List<String> reachable = new ArrayList<>();

        for (var info : handlerMapping.getHandlerMethods().keySet()) {
            if (info.getPathPatternsCondition() == null) {
                continue;
            }

            for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
                if (!pattern.startsWith("/api")) {
                    continue;
                }

                for (RequestMethod method : methodsOf(info)) {
                    String endpoint = method + " " + pattern;
                    if (PUBLIC.contains(endpoint)) {
                        continue;
                    }

                    int status = callAnonymously(method, pattern);
                    if (status != 401 && status != 403) {
                        reachable.add(endpoint + " -> " + status);
                    }
                }
            }
        }

        assertThat(reachable)
                .as("endpoints that answered an anonymous caller with something other than 401/403")
                .isEmpty();
    }

    private Set<RequestMethod> methodsOf(
            org.springframework.web.servlet.mvc.method.RequestMappingInfo info) {
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        // A mapping without an explicit method answers every verb; GET is enough to
        // prove the gate, and the entry point runs before argument resolution.
        return methods.isEmpty() ? Set.of(RequestMethod.GET) : methods;
    }

    private int callAnonymously(RequestMethod method, String pattern) throws Exception {
        String url = pattern.replaceAll("\\{[^}]+}", "1");

        MockHttpServletRequestBuilder request = switch (method) {
            case GET -> get(url);
            case POST -> post(url).contentType(MediaType.APPLICATION_JSON).content("{}");
            case PUT -> put(url).contentType(MediaType.APPLICATION_JSON).content("{}");
            case PATCH -> patch(url).contentType(MediaType.APPLICATION_JSON).content("{}");
            case DELETE -> delete(url);
            default -> null;
        };

        if (request == null) {
            return 401;
        }

        return mockMvc.perform(request).andReturn().getResponse().getStatus();
    }
}