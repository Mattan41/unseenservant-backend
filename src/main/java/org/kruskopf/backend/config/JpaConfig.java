package org.kruskopf.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {


    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    /**
     * The single clock behind every {@code @CreatedDate} / {@code @LastModifiedDate}:
     * UTC, so a row written on a developer's machine and one written in the production
     * container carry the same wall clock. The columns are MySQL {@code datetime},
     * which store no zone, so the zone has to be fixed here.
     * <p>
     * Replaces the framework default ({@code CurrentDateTimeProvider}), which is
     * {@code LocalDateTime.now()} in the JVM's default zone.
     */
    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(ZoneOffset.UTC));
    }
}
