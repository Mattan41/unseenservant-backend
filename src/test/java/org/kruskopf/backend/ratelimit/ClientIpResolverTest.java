package org.kruskopf.backend.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientIpResolver Unit Tests")
class ClientIpResolverTest {

    @Test
    @DisplayName("Prefers what Cloudflare set over anything a proxy added")
    void prefersCloudflareHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.5");
        request.addHeader("CF-Connecting-IP", "203.0.113.7");
        request.addHeader("X-Forwarded-For", "10.0.0.1");
        request.addHeader("X-Real-IP", "10.0.0.2");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("Reads the last X-Forwarded-For entry, not the first")
    void readsLastForwardedEntry() {
        // nginx appends the address it saw with $proxy_add_x_forwarded_for, so
        // everything before the final comma is whatever the client sent. Keying
        // on the first entry would let an attacker invent a fresh budget per
        // request.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.5");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8, 203.0.113.7");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("Falls back to X-Real-IP")
    void fallsBackToRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.5");
        request.addHeader("X-Real-IP", "203.0.113.7");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("Falls back to the peer address when there is no header at all")
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.5");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("172.18.0.5");
    }

    @Test
    @DisplayName("Ignores a header value that cannot be an address")
    void ignoresUnusableValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.5");
        request.addHeader("CF-Connecting-IP", "unknown");
        request.addHeader("X-Forwarded-For", "a".repeat(64));

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("172.18.0.5");
    }

    @Test
    @DisplayName("Never returns a blank key")
    void neverReturnsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("unknown");
    }
}