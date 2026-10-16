package org.kruskopf.backend.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.exception.RateLimitExceededException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("LoginAttemptLimiter Unit Tests")
class LoginAttemptLimiterTest {

    private TestClock clock;
    private RateLimitProperties properties;
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        properties = new RateLimitProperties();
        properties.setWindow(Duration.ofMinutes(1));
        properties.setIpAttempts(3);
        properties.setUsernameAttempts(2);
        limiter = new LoginAttemptLimiter(properties, clock);
    }

    @Nested
    @DisplayName("checkLogin")
    class CheckLogin {

        @Test
        @DisplayName("Allows the budget and refuses the attempt after it")
        void allowsTheBudgetThenRefuses() {
            limiter.checkLogin("203.0.113.7", "user1");
            limiter.checkLogin("203.0.113.7", "user1");

            RateLimitExceededException refusal = assertThrows(RateLimitExceededException.class,
                    () -> limiter.checkLogin("203.0.113.7", "user1"));

            // Never longer than the window, and never zero: a client that retries
            // immediately has to be told a positive number of seconds.
            assertThat(refusal.getRetryAfterSeconds()).isBetween(1L, 60L);
        }

        @Test
        @DisplayName("Counts one account across many addresses")
        void countsOneAccountAcrossAddresses() {
            // The axis a proxy cannot see: the username is in the body, so this
            // is what stops a distributed attack on a single account.
            properties.setIpAttempts(100);

            limiter.checkLogin("203.0.113.1", "user1");
            limiter.checkLogin("203.0.113.2", "user1");

            assertThatThrownBy(() -> limiter.checkLogin("203.0.113.3", "user1"))
                    .isInstanceOf(RateLimitExceededException.class);
        }

        @Test
        @DisplayName("Counts one address across many accounts")
        void countsOneAddressAcrossAccounts() {
            limiter.checkLogin("203.0.113.1", "user1");
            limiter.checkLogin("203.0.113.1", "user2");
            limiter.checkLogin("203.0.113.1", "user3");

            assertThatThrownBy(() -> limiter.checkLogin("203.0.113.1", "user4"))
                    .isInstanceOf(RateLimitExceededException.class);
        }

        @Test
        @DisplayName("Treats a username case-insensitively")
        void treatsUsernameCaseInsensitively() {
            // The lookup is case-insensitive, so these are one account and have
            // to be one budget.
            properties.setIpAttempts(100);

            limiter.checkLogin("203.0.113.1", "User1");
            limiter.checkLogin("203.0.113.1", "user1");

            assertThatThrownBy(() -> limiter.checkLogin("203.0.113.1", "USER1"))
                    .isInstanceOf(RateLimitExceededException.class);
        }

        @Test
        @DisplayName("Leaves the account axis alone when no username is given")
        void ignoresBlankUsername() {
            properties.setIpAttempts(100);

            limiter.checkLogin("203.0.113.1", null);
            limiter.checkLogin("203.0.113.1", "   ");

            // An account that was never named cannot have spent its own budget.
            assertThatCode(() -> limiter.checkLogin("203.0.113.2", "user1"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Restores the budget in the next window")
        void restoresTheBudgetInTheNextWindow() {
            limiter.checkLogin("203.0.113.7", "user1");
            limiter.checkLogin("203.0.113.7", "user1");
            assertThatThrownBy(() -> limiter.checkLogin("203.0.113.7", "user1"))
                    .isInstanceOf(RateLimitExceededException.class);

            clock.advance(Duration.ofMinutes(1));

            assertThatCode(() -> limiter.checkLogin("203.0.113.7", "user1"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Keeps the wait inside the current window")
        void keepsTheWaitInsideTheWindow() {
            limiter.checkLogin("203.0.113.7", "user1");
            limiter.checkLogin("203.0.113.7", "user1");

            clock.advance(Duration.ofSeconds(58));

            RateLimitExceededException refusal = assertThrows(RateLimitExceededException.class,
                    () -> limiter.checkLogin("203.0.113.7", "user1"));

            // Two seconds of the window are left, not sixty.
            assertThat(refusal.getRetryAfterSeconds()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Does nothing when disabled")
        void doesNothingWhenDisabled() {
            properties.setEnabled(false);

            assertThatCode(() -> {
                for (int attempt = 0; attempt < 50; attempt++) {
                    limiter.checkLogin("203.0.113.7", "user1");
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Fails closed once the tracking table is full, and recovers")
        void failsClosedOnceTheTrackingTableIsFull() {
            // Every tracked key is fresh here, which only an attacker cycling
            // addresses can arrange. Refusing is the bounded answer; growing is
            // the memory leak this ceiling exists to prevent.
            properties.setMaxTrackedKeys(4);
            limiter.checkLogin("198.51.100.1", "user1");
            limiter.checkLogin("198.51.100.2", "user2");

            assertThatThrownBy(() -> limiter.checkLogin("192.0.2.1", "user3"))
                    .isInstanceOf(RateLimitExceededException.class);

            // Once the window turns, the stale keys are evicted and the limiter
            // counts normally again.
            clock.advance(Duration.ofMinutes(1));

            assertThatCode(() -> limiter.checkLogin("192.0.2.1", "user3"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("checkOAuthInit")
    class CheckOAuthInit {

        @Test
        @DisplayName("Counts the handshake start per address")
        void countsPerAddress() {
            limiter.checkOAuthInit("203.0.113.1");
            limiter.checkOAuthInit("203.0.113.1");
            limiter.checkOAuthInit("203.0.113.1");

            assertThatThrownBy(() -> limiter.checkOAuthInit("203.0.113.1"))
                    .isInstanceOf(RateLimitExceededException.class);

            // A different visitor is unaffected by it.
            assertThatCode(() -> limiter.checkOAuthInit("203.0.113.2"))
                    .doesNotThrowAnyException();
        }
    }

    /** A clock the test moves by hand, so a window boundary is provable. */
    private static final class TestClock extends Clock {

        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advance(Duration amount) {
            instant = instant.plus(amount);
        }
    }
}