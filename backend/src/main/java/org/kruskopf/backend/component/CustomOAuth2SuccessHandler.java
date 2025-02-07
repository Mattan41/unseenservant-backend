package org.kruskopf.backend.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public CustomOAuth2SuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;

    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        Optional<User> existingUser = userRepository.findByGoogleId(googleId);
        if (existingUser.isEmpty()) {
            userRepository.save(new User(googleId, email, name));
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, String> userData = new HashMap<>();
        userData.put("googleId", googleId);
        userData.put("email", email);
        userData.put("fullName", name);

        response.getWriter().write(new ObjectMapper().writeValueAsString(userData));
        response.getWriter().flush();
    }
}