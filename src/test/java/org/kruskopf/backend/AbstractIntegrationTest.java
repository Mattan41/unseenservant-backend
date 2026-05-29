package org.kruskopf.backend;

import org.kruskopf.backend.config.TestContainersConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base class for integration tests.
 * <p>
 * Provides:
 * - Full Spring Boot context with random port
 * - MySQL Testcontainer (via TestContainersConfiguration)
 * - Test profile activation (loads application-test.properties)
 * - Automatic container lifecycle management
 * <p>
 * All integration tests should extend this class.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(TestContainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    // Subclasses inherit:
    // - Test container MySQL setup
    // - Test profile configuration
    // - Full Spring Security context
    // - Transaction management
}