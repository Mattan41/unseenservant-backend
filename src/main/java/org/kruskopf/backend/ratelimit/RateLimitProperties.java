package org.kruskopf.backend.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Bound from {@code app.rate-limit.*} - see the block in
 * {@code application.properties} for the values and docs/ARCHITECTURE.md
 * ("Login") for the reasoning.
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** Off in the {@code test} profile, so unrelated tests are never throttled. */
    private boolean enabled = true;

    /** Window every counter runs on; a new window restores the budget. */
    private Duration window = Duration.ofMinutes(1);

    /** Attempts per client address per window. */
    private int ipAttempts = 20;

    /** Attempts per username per window, whatever the address it came from. */
    private int usernameAttempts = 5;

    /** Ceiling on tracked keys, so a flood of keys cannot become a memory leak. */
    private int maxTrackedKeys = 10_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public int getIpAttempts() {
        return ipAttempts;
    }

    public void setIpAttempts(int ipAttempts) {
        this.ipAttempts = ipAttempts;
    }

    public int getUsernameAttempts() {
        return usernameAttempts;
    }

    public void setUsernameAttempts(int usernameAttempts) {
        this.usernameAttempts = usernameAttempts;
    }

    public int getMaxTrackedKeys() {
        return maxTrackedKeys;
    }

    public void setMaxTrackedKeys(int maxTrackedKeys) {
        this.maxTrackedKeys = maxTrackedKeys;
    }
}