package org.kruskopf.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Instant;
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
        Instant before = Instant.now();

        // Act
        TemporalAccessor now = provider.getNow().orElseThrow();

        // Assert: a LocalDateTime, because that is what the columns hold.
        assertThat(now).isInstanceOf(LocalDateTime.class);
        LocalDateTime utcNow = (LocalDateTime) now;

        // An offset-free value is a wall clock, so reading it is what gives it a zone.
        // Read as UTC it has to be the current instant...
        Instant readAsUtc = utcNow.toInstant(ZoneOffset.UTC);
        assertThat(readAsUtc).isBetween(before.minusSeconds(5), Instant.now().plusSeconds(5));

        // ...and read in the JVM's zone it must not be. Compare *offsets*, never
        // ZoneIds: a runner whose zone is Etc/UTC has offset zero but is not equal to
        // ZoneOffset.UTC, which is how this test first failed on CI. Where the offset
        // really is zero the two clocks cannot be told apart, so that half proves
        // nothing there - deliberately, and it still pins the value on a machine whose
        // zone differs, which is the case it exists for.
        ZoneOffset defaultOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
        if (!ZoneOffset.UTC.equals(defaultOffset)) {
            assertThat(utcNow.toInstant(defaultOffset)).isNotEqualTo(readAsUtc);
        }
    }
}
