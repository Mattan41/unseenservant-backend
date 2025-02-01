package org.kruskopf.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.component.CustomAuthenticationSuccessHandler;
import org.kruskopf.backend.component.CustomOAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    public SecurityConfig(UserDetailsService userDetailsService,
                          CustomOAuth2SuccessHandler customOAuth2SuccessHandler,
                          CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.customOAuth2SuccessHandler = customOAuth2SuccessHandler;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }



//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http.cors(Customizer.withDefaults()).csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> {
//                    auth.requestMatchers("/admin").hasRole("ADMIN");
//                    auth.requestMatchers("/api/users/data").authenticated();
//                    auth.requestMatchers("/api/login", "/api/users","/home").permitAll();
//                    auth.anyRequest().denyAll();
//                }).exceptionHandling(exceptionHandling ->
//                        exceptionHandling
//                                .accessDeniedHandler((request, response, accessDeniedException) -> {
//                                    if (request.getUserPrincipal() != null) {
//                                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
//                                    } else {
//                                        response.sendRedirect("/login");
//                                    }
//                                }))
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessUrl("http://localhost:5173/")
//                        .invalidateHttpSession(true)
//                        .deleteCookies("JSESSIONID"))
//                .oauth2Login(oauth2 -> oauth2
//                        .successHandler(customOAuth2SuccessHandler))
//                .formLogin(form -> form
//                        .successHandler(customFormLoginSuccessHandler));
//        return http.build();
//    }

    // SecurityConfig.java //todo: remove csrf.(AbstractHttpConfigurer::disable)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults()).csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/admin").hasRole("ADMIN");
                    auth.requestMatchers("/api/auth/**").permitAll(); //.anyRequest().authenticated() Temporarily allow access without authentication
                    auth.requestMatchers("/api/login", "/api/users", "/home").permitAll();
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
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))

//                .oauth2Login(oauth2 -> oauth2
//                        .successHandler(customOAuth2SuccessHandler))
//                        .loginProcessingUrl("/api/auth/login")
                .formLogin(form -> form
                        .successHandler(customAuthenticationSuccessHandler));

        return http.build();
    }


    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173", "https://unseenservant.se")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

}

