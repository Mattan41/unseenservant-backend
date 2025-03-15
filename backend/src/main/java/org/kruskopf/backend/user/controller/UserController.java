package org.kruskopf.backend.user.controller;

import org.kruskopf.backend.user.CustomUserDetails;
import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    public UserService userService;

    // todo: add @PreAuthorize on endponints after testing with Postman is done
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getLoggedInUser(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

        String email = customUserDetails.getUsername(); // uses the email which is unique
        User user = userService.findByUserName(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Convert User to UserDTO before returning
        UserDTO userDTO = userService.toDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {

        User user = userService.findById(id).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        UserDTO userDTO = userService.toDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
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