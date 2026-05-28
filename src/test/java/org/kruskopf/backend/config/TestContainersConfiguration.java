
package org.kruskopf.backend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration that provides a MySQL Testcontainer for integration tests.
 * <p>
 * Spring Boot 3.1+ automatically configures DataSource from @ServiceConnection.
 * The container is reused between tests for better performance.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

    @Bean
    @ServiceConnection
    public MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0.42"))
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass")
                .withReuse(true); // Reuse container across test runs
    }
}