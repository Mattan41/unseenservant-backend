
package org.kruskopf.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.auth.JwtAuthenticationFilter;
import org.kruskopf.backend.component.CustomOAuth2SuccessHandler;
import org.kruskopf.backend.user.entity.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableResilientMethods
public class SecurityConfig {

    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${ALLOWED_ORIGINS}")
    private String[] allowedOrigins;

    public SecurityConfig(CustomOAuth2SuccessHandler customOAuth2SuccessHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectMapper objectMapper) {
        this.customOAuth2SuccessHandler = customOAuth2SuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    /**
     * The 401 and 403 the filter chain answers with — refused before any
     * controller runs, so {@code GlobalExceptionHandler} never sees them. They are
     * written here instead, in the same shape it uses: an RFC 9457 body whose
     * {@code detail} is the sentence a client can show.
     * <p>
     * Done directly on the response rather than by throwing, because there is no
     * Advice at this point in the chain; {@code response.sendError} was the
     * alternative and it delegates to the container's HTML error page.
     */
    private void writeProblem(HttpServletRequest request, HttpServletResponse response,
                              HttpStatus status, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile("!demo")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    // "/error" has to be permitted: when a permitted path has no
                    // handler (a missing upload, an unknown URL) or a controller
                    // fails, the container re-dispatches to /error and this filter
                    // chain runs again on that dispatch. Without the matcher the
                    // second pass answers 401 and the real status is masked — a
                    // missing image looks like a rejected token. The demo chain
                    // below lists it for the same reason.
                    auth.requestMatchers("/", "/oauth2/**", "/error").permitAll();
                    auth.requestMatchers("/images/**").permitAll();
                    // Administrator surface. The pattern matters: an exact "/api/admin"
                    // matcher would only cover the bare path and leave every endpoint
                    // below it to anyRequest().denyAll().
                    auth.requestMatchers("/api/admin/**").hasRole(UserRole.ADMIN.name());
                    // The specific matcher must come before the broad one: Spring Security
                    // takes the FIRST match, so "/api/auth/**" would otherwise make
                    // /api/auth/me public and leave it to the controller's own check.
                    auth.requestMatchers("/api/auth/me").authenticated();
                    auth.requestMatchers("/api/auth/**").permitAll();
                    auth.requestMatchers("/api/users/**", "/api/campaigns/**", "/api/characters/**", "/api/messages/**","/api/spells/**").authenticated();
                    auth.anyRequest().denyAll();
                })
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint((request, response, authException) ->
                                        writeProblem(request, response, HttpStatus.UNAUTHORIZED,
                                                "Authentication is required."))
                                .accessDeniedHandler((request, response, accessDeniedException) ->
                                        writeProblem(request, response, HttpStatus.FORBIDDEN,
                                                "Access denied.")))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customOAuth2SuccessHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Profile("demo")
    public SecurityFilterChain demoSecurityFilterChain(HttpSecurity http) {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/oauth2/**", "/h2-console/**", "/favicon.ico",
                            "/error").permitAll();
                    auth.requestMatchers("/images/**").permitAll();
                    // Administrator surface — see the non-demo chain above for why the
                    // pattern, and not the bare prefix, is the matcher that matters.
                    auth.requestMatchers("/api/admin/**").hasRole(UserRole.ADMIN.name());
                    // Same ordering rule as the non-demo chain: the specific matcher first.
                    auth.requestMatchers("/api/auth/me").authenticated();
                    auth.requestMatchers("/api/auth/**").permitAll();
                    auth.requestMatchers("/api/users/**", "/api/campaigns/**", "/api/characters/**", "/api/messages/**","/api/spells/**").authenticated();
                    auth.anyRequest().denyAll();
                })
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint((request, response, authException) ->
                                        writeProblem(request, response, HttpStatus.UNAUTHORIZED,
                                                "Authentication is required."))
                                .accessDeniedHandler((request, response, accessDeniedException) ->
                                        writeProblem(request, response, HttpStatus.FORBIDDEN,
                                                "Access denied.")))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .oauth2Login(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("USER")
                .role("USER").implies("GUEST")
                .build();
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }

    @Bean
    public RestClient restClient() {
        RestClient restClient = RestClient.create();
        customOAuth2SuccessHandler.setRestClient(restClient);
        return restClient;
    }
}