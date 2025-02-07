package org.kruskopf.backend.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> loggedInUserData(Authentication authentication) {
        if (authentication == null) {
            logger.warn("Authentication is null");
            return ResponseEntity.badRequest().build();
        }
        logger.info("Fetching logged-in user data for: {}", authentication.getName());
        Map<String, String> data = new HashMap<>();
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        data.put("googleId", googleId);
        data.put("email", email);
        data.put("name", name);
        return ResponseEntity.ok(data);
    }


    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        User user = userService.findByUserName(username);
        if (user == null || !userService.passwordEncoder().matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body(Map.of("message", "Invalid credentials"));
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("username", user.getUserName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/google-login")
    public ResponseEntity<User> one(@RequestParam String googleId) {
        return ResponseEntity.ok().body(userService.find(googleId));
    }

    @PostMapping("/google-login")
    public ResponseEntity<User> save(@RequestBody User user) {
        return ResponseEntity.ok().body(userService.save(user));
    }
}

