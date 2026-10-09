package org.kruskopf.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the full application context with a Flyway-managed schema and Hibernate
 * {@code ddl-auto=validate}.
 * <p>
 * The other integration tests run with {@code create-drop}, which lets Hibernate
 * build the schema and therefore hides any drift between the Flyway migrations
 * and the JPA entities (or a broken/duplicated migration). This test exercises
 * the same path the {@code develop}/{@code prod} profiles use, so a schema
 * mismatch or a Flyway checksum/validation failure surfaces here.
 * <p>
 * It deliberately declares its own <em>non-reused</em> container: a reused
 * container can be left holding a Flyway history from a {@code create-drop} run
 * whose tables were dropped on shutdown, which would make validation fail for
 * the wrong reason.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
@DisplayName("Flyway + Hibernate schema validation")
class SchemaValidationIT {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.42")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Test
    @DisplayName("Context starts with migrations applied and entities validated against the schema")
    void contextLoadsWithValidatedSchema() {
        // Intentionally empty: the assertion is that the context starts at all.
        // If Flyway fails to validate/apply migrations, or Hibernate finds the
        // schema does not match the entities, context startup fails and so does
        // this test.
    }
}
