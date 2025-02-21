package org.kruskopf.backend.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");

        // todo: solve form login , DTO ??

        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        Map<String, String> userData = new HashMap<>();

        if (roles.contains("ROLE_ADMIN")) {
            // Handle admin user
            userData.put("role", "admin");
            response.getWriter().write(new ObjectMapper().writeValueAsString(userData));
            response.getWriter().flush();
            return;
        } else if (authentication.getPrincipal() instanceof User user) {
            // Handle regular user
            userData.put("username", user.getUsername());
        }

        // Write user data to response
        response.getWriter().write(new ObjectMapper().writeValueAsString(userData));
        response.getWriter().flush();
    }
}