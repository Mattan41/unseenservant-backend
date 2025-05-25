package org.kruskopf.backend.user.controller;

import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.user.CustomUserDetails;
import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    public UserService userService;

    // todo: add @PreAuthorize on relevant endpoints
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUsers(
            @RequestParam String query,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long currentUserId = customUserDetails.user().getId();
        List<UserDTO> users = userService.searchUsers(query, currentUserId);
        return ResponseEntity.ok(users);
    }

// preauthorize that only the logged in user can access their own data
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
        User updatedUser = userService.partialUpdate(id, updates)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return ResponseEntity.ok(updatedUser);
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<User> updateProfile(@PathVariable Long id, @RequestBody User user) {
        return userService.update(id, user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



}