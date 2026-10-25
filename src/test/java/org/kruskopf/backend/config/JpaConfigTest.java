package org.kruskopf.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JpaConfig Unit Tests")
class JpaConfigTest {

    private final JpaConfig config = new JpaConfig();

    @Test
    @DisplayName("The audit clock is UTC, not the JVM's default zone")
    void auditClockIsUtc() {
        // Arrange
        DateTimeProvider provider = config.auditingDateTimeProvider();

        // Act
        TemporalAccessor now = provider.getNow().orElseThrow();

        // Assert: a LocalDateTime (what the columns hold) and the UTC wall clock.
        assertThat(now).isInstanceOf(LocalDateTime.class);
        LocalDateTime utcNow = (LocalDateTime) now;
        assertThat(Duration.between(utcNow, LocalDateTime.now(ZoneOffset.UTC)).abs())
                .isLessThan(Duration.ofSeconds(5));

        // A CI container already runs in UTC, where the two clocks cannot be told
        // apart, so this only means something where the default zone really differs.
        ZoneId defaultZone = ZoneId.systemDefault();
        if (!defaultZone.equals(ZoneOffset.UTC)) {
            assertThat(Duration.between(utcNow, LocalDateTime.now(defaultZone)).abs())
                    .isGreaterThan(Duration.ofMinutes(1));
        }
    }
}
