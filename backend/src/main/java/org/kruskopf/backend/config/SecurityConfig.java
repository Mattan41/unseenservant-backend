package org.kruskopf.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.component.CustomOAuth2SuccessHandler;
import org.kruskopf.backend.user.entity.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableRetry
public class SecurityConfig {

    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    public SecurityConfig(
            CustomOAuth2SuccessHandler customOAuth2SuccessHandler) {
        this.customOAuth2SuccessHandler = customOAuth2SuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                //.csrf(AbstractHttpConfigurer::disable) // Disable CSRF protection if needed for manual testing todo: remove this line
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/oauth2/**", "/logout").permitAll();
                    auth.requestMatchers("/admin").hasRole(UserRole.ADMIN.name());
                    auth.requestMatchers("/api/auth/**", "/api/auth/login").permitAll();
                    auth.requestMatchers("/api/auth/me").authenticated();
                    auth.requestMatchers("/api/users/**", "/api/campaigns/**", "/api/characters/**", "/api/messages/**").authenticated();
                    // auth.anyRequest().permitAll(); // switch for postman testing without need for authentication todo: remove this line
                    auth.anyRequest().denyAll();

                }).exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                    if (request.getUserPrincipal() != null) {
                                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                                    } else {
                                        response.sendRedirect("/login");
                                    }
                                }))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .expiredUrl("/login"))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .formLogin(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(customOAuth2SuccessHandler));

        return http.build();
    }


    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173", "https://unseenservant.se")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .exposedHeaders("Set-Cookie");
            }
        };
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("USER")
                .role("USER").implies("GUEST")
                .build();
    }

    // this is for using Pre and PostAuthorize annotations, it is needed for Role hierarchy to work with these annotations
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

