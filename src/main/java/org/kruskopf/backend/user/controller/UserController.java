package org.kruskopf.backend.user.controller;

import jakarta.validation.Valid;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.user.CustomUserDetails;
import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.dto.UserProfileUpdateDTO;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final String USER_NOT_FOUND = "User not found with id: ";

    public final UserService userService;

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

    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getLoggedInUser(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        // Thrown, not returned empty: the advice turns either into a 404, and only
        // a thrown one carries a detail the client can show.
        User user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));

        UserDTO userDTO = userService.toDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));

        UserDTO userDTO = userService.toDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PatchMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserProfileUpdateDTO request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        boolean isAdmin = currentUser.user().getRole() == UserRole.ADMIN;
        if (!currentUser.user().getId().equals(id) && !isAdmin) {
            throw new UnauthorizedAccessException("Only the owner of a profile or an administrator may edit it.");
        }

        // Refused rather than ignored: both fields used to be writable through this
        // endpoint, and the email in particular must not be self-service.
        if (request.userName() != null || request.email() != null) {
            throw new InvalidRequestException(
                    "Only the display name can be changed here. An email change needs a verified flow, "
                            + "and the username is the form-login identity.");
        }

        User updatedUser = userService.updateDisplayName(id, request.displayName())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
        UserDTO userDTO = userService.toDTO(updatedUser);
        return ResponseEntity.ok(userDTO);
    }
}
