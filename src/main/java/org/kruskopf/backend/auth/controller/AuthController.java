package org.kruskopf.backend.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.auth.JwtService;
import org.kruskopf.backend.auth.dto.AuthDTO;
import org.kruskopf.backend.auth.dto.LoginRequest;
import org.kruskopf.backend.ratelimit.ClientIpResolver;
import org.kruskopf.backend.ratelimit.LoginAttemptLimiter;
import org.kruskopf.backend.user.CustomUserDetails;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import org.kruskopf.backend.user.entity.User;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptLimiter loginAttemptLimiter;

    /**
     * Compared when the username does not exist, so an unknown account costs the
     * same hashing work as a known one. See docs/ARCHITECTURE.md ("Login").
     */
    private final String unknownUserHash;

    @Value("${ALLOWED_ORIGINS}")
    private String[] allowedOrigins;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          LoginAttemptLimiter loginAttemptLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request) {
        // Throttle first: the lookup and the hash comparison below are the cost
        // it protects. See docs/ARCHITECTURE.md ("Login").
        loginAttemptLimiter.checkLogin(ClientIpResolver.resolve(request), loginRequest.username());

        var found = userRepository.findByUserName(loginRequest.username());

        // One comparison either way, so an unknown username cannot be told from a
        // wrong password by the work it costs. See docs/ARCHITECTURE.md ("Login").
        boolean matches = passwordEncoder.matches(
                loginRequest.password(),
                found.map(User::getPassword).orElse(unknownUserHash));

        if (found.isEmpty() || !matches) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = found.get();
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(new AuthDTO(
                        user.getId(),
                        user.getUserName(),
                        user.getEmail(),
                        user.getRole().name()
                ));
    }

    @GetMapping("/oauth-init")
    public void initiateOAuth(
            @RequestParam String provider,
            @RequestParam String origin,
            HttpServletResponse response,
            HttpServletRequest request
    ) throws IOException {
        loginAttemptLimiter.checkOAuthInit(ClientIpResolver.resolve(request));

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

