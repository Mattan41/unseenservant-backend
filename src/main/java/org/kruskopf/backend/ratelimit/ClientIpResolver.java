package org.kruskopf.backend.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The client address a throttle should count against, in this order:
 * {@code CF-Connecting-IP}, the <b>last</b> {@code X-Forwarded-For} entry (nginx
 * appends the address it saw, so everything before that comma is client-supplied),
 * {@code X-Real-IP}, then the peer address.
 * <p>
 * None of it is a trust boundary on its own - all three headers can be forged by
 * whoever reaches this port, which is why the backend port stays bound to
 * loopback. See docs/DEPLOYMENT.md ("Rate limiting") and docs/ARCHITECTURE.md
 * ("Login").
 */
public final class ClientIpResolver {

    /** Longest textual address worth keeping: an IPv6 literal, brackets and all. */
    private static final int MAX_LENGTH = 45;

    private ClientIpResolver() {
        // Utility class
    }

    /** @return a non-blank key, never longer than {@value #MAX_LENGTH} characters */
    public static String resolve(HttpServletRequest request) {
        String cloudflare = usable(request.getHeader("CF-Connecting-IP"));
        if (cloudflare != null) {
            return cloudflare;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            int lastSeparator = forwardedFor.lastIndexOf(',');
            String last = usable(
                    lastSeparator >= 0 ? forwardedFor.substring(lastSeparator + 1) : forwardedFor);
            if (last != null) {
                return last;
            }
        }

        String realIp = usable(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        return truncate(request.getRemoteAddr());
    }

    private static String usable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()
                || trimmed.length() > MAX_LENGTH
                || trimmed.indexOf(',') >= 0
                || "unknown".equalsIgnoreCase(trimmed)) {
            return null;
        }

        return trimmed;
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH);
    }
}