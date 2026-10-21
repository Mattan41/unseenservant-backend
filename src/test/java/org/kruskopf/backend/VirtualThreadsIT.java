package org.kruskopf.backend;

import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the virtual-thread configuration.
 * <p>
 * Enabling virtual threads moves the concurrency ceiling away from Tomcat's
 * worker-thread pool and onto {@code max-connections} and the Hikari pool (see
 * {@code docs/ARCHITECTURE.md}). That is easy to undo by accident, so this test
 * pins both the setting and the resulting behaviour.
 * <p>
 * It runs against a real Tomcat instance ({@code RANDOM_PORT} via
 * {@link AbstractIntegrationTest}) rather than MockMvc, because MockMvc never
 * enters the servlet container and would prove nothing about thread handling.
 */
@AutoConfigureTestRestTemplate
@DisplayName("Virtual thread request handling")
class VirtualThreadsIT extends AbstractIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("spring.threads.virtual.enabled is on")
    void virtualThreadsAreEnabled() {
        assertThat(environment.getProperty("spring.threads.virtual.enabled", Boolean.class))
                .as("Virtual threads must stay on: without them Tomcat's worker-thread pool "
                        + "becomes the concurrency ceiling again and max-connections is not the "
                        + "bound that docs/ARCHITECTURE.md describes")
                .isTrue();
    }

    @Test
    @DisplayName("Tomcat's connector runs on a virtual-thread executor")
    void tomcatUsesVirtualThreadExecutor() {
        TomcatWebServer webServer =
                (TomcatWebServer) ((WebServerApplicationContext) applicationContext).getWebServer();
        Executor executor = webServer.getTomcat().getConnector().getProtocolHandler().getExecutor();

        assertThat(executor)
                .as("The connector should use Tomcat's VirtualThreadExecutor, not the default "
                        + "thread pool")
                .isInstanceOf(VirtualThreadExecutor.class);
    }

    @Test
    @DisplayName("A request is served on a virtual thread")
    void requestIsHandledOnAVirtualThread() {
        ResponseEntity<ThreadProbe> response = restTemplate.getForEntity("/", ThreadProbe.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ThreadProbe body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.virtual())
                .as("Request was handled on '%s', which is not a virtual thread", body.name())
                .isTrue();
    }

    /** Response of the test-only {@link ThreadProbeController}. */
    record ThreadProbe(boolean virtual, String name) {
    }

    @TestConfiguration
    static class ThreadProbeConfiguration {

        @Bean
        ThreadProbeController threadProbeController() {
            return new ThreadProbeController();
        }
    }

    /**
     * Test-only probe reporting whether the serving thread is virtual.
     * <p>
     * Mapped to {@code /} because both security filter chains already permit that
     * path and neither uses it, while the chains end with
     * {@code anyRequest().denyAll()}.
     */
    @RestController
    static class ThreadProbeController {

        @GetMapping("/")
        ThreadProbe thread() {
            Thread current = Thread.currentThread();
            return new ThreadProbe(current.isVirtual(), current.getName());
        }
    }
}
