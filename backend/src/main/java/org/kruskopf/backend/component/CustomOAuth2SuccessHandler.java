package org.kruskopf.backend.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Hibernate;
import org.kruskopf.backend.user.UserRole;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public CustomOAuth2SuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;

    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        User user = userRepository.findByProviderId(oidcUser.getSubject())
                .orElseGet(() -> userRepository.save(new User(
                        oidcUser.getSubject(),
                        ProviderType.GOOGLE,
                        oidcUser.getEmail(),
                        oidcUser.getFullName(),
                        oidcUser.getEmail(),
                        UserRole.ROLE_USER, // Default role
                        "password" // Default password
                )));

        // Initialize lazy loaded collections
        Hibernate.initialize(user.getCharacters());
        Hibernate.initialize(user.getMessages());

        // Create session and JSESSIONID cookie is created automatically Todo: migrate to UserDTO, MessageDTO and CharacterDTO, remove Transactional annotation
        request.getSession().setAttribute("user", user);

        // todo replace hardcoded url with env variable or investigate if possibble to use relative path and configure spring/vue
        response.sendRedirect("http://localhost:5173/oauth-redirect");

    }
}