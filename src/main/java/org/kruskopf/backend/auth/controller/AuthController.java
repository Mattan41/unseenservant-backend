package org.kruskopf.backend.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.auth.dto.AuthDTO;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${ALLOWED_ORIGINS}")
    private String[] allowedOrigins;

    @GetMapping("/oauth-init")
    public void initiateOAuth(
            @RequestParam String provider,
            @RequestParam String origin,
            HttpServletResponse response
    ) throws IOException {
        if (!Set.of("google", "github").contains(provider)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid provider");
            return;
        }

        if (Arrays.asList(allowedOrigins).contains(origin)) {
            ResponseCookie cookie = ResponseCookie.from("oauth_redirect_origin", origin)
                    .path("/")
                    .httpOnly(true)
                    .maxAge(300)
                    .secure(true)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        AuthDTO authDTO = new AuthDTO(
                userDetails.user().getId(),
                userDetails.getUsername(),
                userDetails.user().getEmail(),
                userDetails.getAuthorities().iterator().next().getAuthority()
        );

        return ResponseEntity.ok(authDTO);
    }
}

