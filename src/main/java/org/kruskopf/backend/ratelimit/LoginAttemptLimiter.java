package org.kruskopf.backend.ratelimit;

import org.kruskopf.backend.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fixed-window throttle for the unauthenticated auth endpoints, keyed on the
 * client address and on the username, in memory and per-instance. The bounded map
 * is what keeps it from becoming a leak. See docs/ARCHITECTURE.md ("Login").
 */
@Component
public class LoginAttemptLimiter {

    private final RateLimitProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /** The injected constructor; the second one exists for tests. */
    @Autowired
    public LoginAttemptLimiter(RateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /** Visible for tests: a fixed clock is what makes a window boundary testable. */
    LoginAttemptLimiter(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Counts one login attempt on both axes.
     *
     * @throws RateLimitExceededException when either axis has no budget left
     */
    public void checkLogin(String clientIp, String username) {
        if (!properties.isEnabled()) {
            return;
        }

        long now = clock.millis();
        long retryAfter = consume("ip:" + clientIp, properties.getIpAttempts(), now);

        if (retryAfter == 0 && username != null && !username.isBlank()) {
            // One account is one budget: the lookup is case-insensitive.
            retryAfter = consume(
                    "user:" + username.trim().toLowerCase(Locale.ROOT),
                    properties.getUsernameAttempts(),
                    now);
        }

        if (retryAfter > 0) {
            throw new RateLimitExceededException(retryAfter);
        }
    }

    /**
     * Counts one OAuth handshake start, on the address axis only.
     *
     * @throws RateLimitExceededException when the address has no budget left
     */
    public void checkOAuthInit(String clientIp) {
        if (!properties.isEnabled()) {
            return;
        }

        long retryAfter = consume("oauth:" + clientIp, properties.getIpAttempts(), clock.millis());
        if (retryAfter > 0) {
            throw new RateLimitExceededException(retryAfter);
        }
    }

    /** @return 0 when the request may proceed, otherwise the seconds to wait */
    private long consume(String key, int limit, long nowMillis) {
        long windowMillis = Math.max(1, properties.getWindow().toMillis());

        if (windows.size() >= properties.getMaxTrackedKeys()) {
            evictExpired(nowMillis, windowMillis);

            if (windows.size() >= properties.getMaxTrackedKeys()) {
                // Only an attacker cycling addresses can fill the table with live
                // keys, so make the request wait rather than grow with it.
                return Math.max(1, windowMillis / 1000);
            }
        }

        AtomicLong retryAfter = new AtomicLong();
        windows.compute(key, (tracked, existing) -> {
            if (existing == null || nowMillis - existing.windowStartMillis() >= windowMillis) {
                return new Window(nowMillis, 1);
            }

            if (existing.count() < limit) {
                return new Window(existing.windowStartMillis(), existing.count() + 1);
            }

            long remainingMillis = windowMillis - (nowMillis - existing.windowStartMillis());
            retryAfter.set(Math.max(1, (remainingMillis + 999) / 1000));
            return existing;
        });

        return retryAfter.get();
    }

    private void evictExpired(long nowMillis, long windowMillis) {
        windows.entrySet().removeIf(
                entry -> nowMillis - entry.getValue().windowStartMillis() >= windowMillis);
    }

    private record Window(long windowStartMillis, int count) {
    }
}