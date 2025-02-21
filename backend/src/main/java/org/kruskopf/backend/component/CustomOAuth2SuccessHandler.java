package org.kruskopf.backend.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.auth.dto.AuthDTO;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
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

        // Handle role with whitelist todo add a email to whitelist and to env variable
        String adminWhitelist = System.getenv("ADMIN_WHITELIST"); // T.ex. "admin@example.com"
        boolean isAdmin = adminWhitelist != null && adminWhitelist.contains(oidcUser.getEmail());

        UserRole role = isAdmin ? UserRole.ADMIN : UserRole.USER;

        // Hämta eller skapa ny användare
        User user = userRepository.findByProviderId(oidcUser.getSubject())
                .orElseGet(() -> userRepository.save(new User(
                        oidcUser.getSubject(),
                        ProviderType.GOOGLE,
                        oidcUser.getEmail(),
                        oidcUser.getFullName(),
                        oidcUser.getEmail(),
                        role,
                        "password"
                )));

        // Skapa AuthDTO för session
        AuthDTO authDTO = new AuthDTO(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole().toString()
        );
        request.getSession().setAttribute("user", authDTO);

        // Dynamisk redirect
        String redirectUrl = System.getenv("FRONTEND_REDIRECT_URL");
        if (redirectUrl == null) {
            redirectUrl = "http://localhost:5173";
        }
        response.sendRedirect(redirectUrl + "/oauth-redirect");
    }
}

//
// todo: Is password really needed? Using social login, password is not needed. Investigate if possible to remove password from User entity
//
// todo replace hardcoded url with env variable or investigate if possibble to use relative path and configure spring/vue
