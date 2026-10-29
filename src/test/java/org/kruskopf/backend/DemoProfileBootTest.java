package org.kruskopf.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code demo} profile boots: H2 in memory, Flyway off, the schema generated
 * from the JPA entities, and {@code demo-data.sql} seeded on top.
 * <p>
 * That is the only path in the project where Hibernate <em>generates</em> the
 * schema, and nothing else covers it: every integration test runs against MySQL,
 * where Flyway owns the schema, so the entity's own DDL is never executed. A
 * mapping mistake that shows up only in generated DDL therefore passes the whole
 * suite — which is how a {@code @UniqueConstraint} naming a column that does not
 * exist (the property {@code date}, the column {@code plan_date}) went unnoticed
 * while it stopped {@code npm run demo} from starting at all.
 * <p>
 * Booting is a real assertion, because Hibernate treats a failed DDL statement as
 * a warning and carries on: the damage surfaces one step later, when the seed
 * inserts into a table that was never created and startup aborts.
 * <p>
 * No container is needed, so this runs with the unit tests rather than with the
 * integration tests.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "JWT_SECRET=demo-profile-boot-test-secret-key-at-least-32-bytes",
                // The demo profile logs every statement; the schema and the seed are
                // the same either way, and the report stays readable without it.
                "spring.jpa.show-sql=false",
        })
@ActiveProfiles("demo")
@DisplayName("demo profile boot test")
class DemoProfileBootTest {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    DemoProfileBootTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * The demo profile's two promises in one test: the entities' generated schema
     * can be created, and the seed lands in it. Both matter — a context that
     * starts without its seed would leave Guest Demo's sibling (Demo Mode) with no
     * accounts to log in as, which is not something the suite would otherwise
     * notice.
     */
    @Test
    @DisplayName("Context starts with the entity-generated schema and the seeded data")
    void startsWithTheGeneratedSchemaAndTheSeededData() {
        Integer accounts = jdbcTemplate.queryForObject("select count(*) from users", Integer.class);
        Integer dates = jdbcTemplate.queryForObject("select count(*) from planner_date", Integer.class);

        // The six seeded accounts (User1–User5 and admin), and the planner dates the
        // planner section demos with.
        assertThat(accounts).isEqualTo(6);
        assertThat(dates).isNotZero();
    }
}
