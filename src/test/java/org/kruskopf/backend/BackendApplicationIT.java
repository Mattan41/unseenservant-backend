package org.kruskopf.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.testcontainers.containers.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests to verify that the application context loads correctly
 * with Testcontainers and all necessary beans are present.
 */
@DisplayName("Backend Application Context Tests")
class BackendApplicationIT extends AbstractIntegrationTest {

    private final ApplicationContext applicationContext;

    @Autowired
    BackendApplicationIT(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBeanDefinitionCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("MySQL Test container should be running")
    void mysqlContainerIsRunning() {
        MySQLContainer<?> container = applicationContext.getBean(MySQLContainer.class);
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
    }

    @Test
    @DisplayName("Security configuration beans should be present")
    void securityBeansArePresent() {
        assertThat(applicationContext.containsBean("securityFilterChain")).isTrue();
        assertThat(applicationContext.containsBean("passwordEncoder")).isTrue();
    }

    @Test
    @DisplayName("JWT authentication filter should be present")
    void jwtAuthenticationFilterIsPresent() {
        assertThat(applicationContext.containsBean("jwtAuthenticationFilter")).isTrue();
    }

}