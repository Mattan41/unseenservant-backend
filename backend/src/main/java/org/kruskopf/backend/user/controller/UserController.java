package org.kruskopf.backend.user.controller;

import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    public UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getLoggedInUser(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String providerId = oidcUser.getSubject();
        User user = userService.find(providerId);

        // Kontrollera om användaren finns i databasen
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Convert User to UserDTO before returning
        UserDTO userDTO = userService.toDTO(user);
        return ResponseEntity.ok(userDTO);

    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id, @AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User user = userService.findById(id).orElse(null);

        if (user == null || !user.getEmail().equals(oidcUser.getEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserDTO userDTO = userService.toDTO(user);
        return ResponseEntity.ok(userDTO);
    }


    @PatchMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return userService.partialUpdate(id, updates)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateProfile(@PathVariable Long id, @RequestBody User user) {
        return userService.update(id, user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



}