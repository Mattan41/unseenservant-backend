package org.kruskopf.backend.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.auth.dto.AuthDTO;
import org.kruskopf.backend.user.CustomUserDetails;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

        // Check for admin role in whitelist
        String adminWhitelist = System.getenv("ADMIN_WHITELIST");
        boolean isAdmin = adminWhitelist != null && adminWhitelist.contains(oidcUser.getEmail());

        UserRole role = isAdmin ? UserRole.ADMIN : UserRole.USER;

        // Find or Create the user in the database
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

        // Update Spring Security Context with new Authentication (using the user from DB)
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(authentication.getDetails());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Set user data (AuthDTO) into session for easy frontend communication
        AuthDTO authDTO = new AuthDTO(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole().toString()
        );
        request.getSession().setAttribute("user", authDTO);

        // Redirect to frontend
        String redirectUrl = System.getenv("FRONTEND_REDIRECT_URL");
        if (redirectUrl == null) {
            redirectUrl = "http://localhost:5173";
        }
        response.sendRedirect(redirectUrl + "/oauth-redirect");
    }
}

// todo replace hardcoded url with env variable or investigate if possibble to use relative path and configure spring/vue.
//  verify that System.getenv works for email whitelist and frontendurl or find another way
