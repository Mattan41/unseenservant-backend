package org.kruskopf.backend.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.auth.dto.AuthDTO;
import org.kruskopf.backend.user.CustomUserDetails;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
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

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> loggedInUserData(@AuthenticationPrincipal OidcUser oidcUser, Authentication authentication) {
        if (authentication == null) {
            logger.warn("Authentication is null");
            return ResponseEntity.badRequest().build();
        }
        logger.info("Fetching logged-in user data for: {}", authentication.getName());
        Map<String, String> data = new HashMap<>();

        String providerId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        data.put("providerId", providerId);
        data.put("email", email);
        data.put("name", name);
        return ResponseEntity.ok(data);
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        User user = userService.findByUserName(username);
        if (user == null || !passwordEncoder().matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(Map.of("message", "Invalid credentials"));
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("id", String.valueOf(user.getId())); // Lägg till användarens ID
        response.put("username", user.getUserName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());
        return ResponseEntity.ok(response);
    }
}

