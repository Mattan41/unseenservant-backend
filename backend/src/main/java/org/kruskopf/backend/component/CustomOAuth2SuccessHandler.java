package org.kruskopf.backend.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public CustomOAuth2SuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;

    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        // Save user to database if not exists
        User user = userRepository.findByGoogleId(oidcUser.getSubject())
                .orElseGet(() -> userRepository.save(new User(
                        oidcUser.getSubject(),
                        oidcUser.getEmail(),
                        oidcUser.getFullName()
                )));

        // Create session and JSESSIONID cookie is created automatically
        request.getSession().setAttribute("user", user);

        // todo replace hardcoded url with env variable or investigate if possibble to use relative path and configure spring/vue
        response.sendRedirect("http://localhost:5173/oauth-redirect");

    }
}