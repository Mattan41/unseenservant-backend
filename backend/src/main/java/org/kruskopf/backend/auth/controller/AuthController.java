package org.kruskopf.backend.auth.controller;

import org.kruskopf.backend.auth.dto.AuthDTO;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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

