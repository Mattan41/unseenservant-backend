package org.kruskopf.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

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
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.11")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Context starts with migrations applied and entities validated against the schema")
    void contextLoadsWithValidatedSchema() {
        // Intentionally empty: the assertion is that the context starts at all.
        // If Flyway fails to validate/apply migrations, or Hibernate finds the
        // schema does not match the entities, context startup fails and so does
        // this test.
    }

    /**
     * A character may have no game system, which is stored as NULL — but the
     * column may still only hold a system the code can render. The baseline adds
     * that CHECK; this pins it so it cannot quietly disappear in a later migration.
     */
    @Test
    @DisplayName("system_type is constrained to NULL or a registered game system")
    void systemTypeCheckConstraintIsPresent() {
        String clause = jdbcTemplate.queryForObject(
                "SELECT CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS "
                        + "WHERE CONSTRAINT_SCHEMA = DATABASE() "
                        + "AND CONSTRAINT_NAME = 'chk_character_system_type'",
                String.class);

        assertThat(clause.toLowerCase())
                .contains("system_type")
                .contains("is null")
                .contains("dnd5e")
                .contains("offworlders");
    }
}
