package org.kruskopf.backend.user.controller;

import org.kruskopf.backend.user.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    public UserService userService;


    public UserController(UserService userService) {
        this.userService = userService;
    }

//These endpoints might be redundant because of auth controller endpoints
//    @GetMapping
//    public ResponseEntity<User> one(@RequestParam String googleId) {
//        return ResponseEntity.ok().body(userService.find(googleId));
//    }
//
//    @PostMapping
//    public ResponseEntity<User> save(@RequestBody User user) {
//        return ResponseEntity.ok().body(userService.save(user));
//    }

//    @GetMapping("/data")
//    public ResponseEntity<Map<String, String>> loggedInUserData(Authentication authentication) {
//        if (authentication == null)
//            return ResponseEntity.badRequest().build();
//        Map<String, String> data = new HashMap<>();
//        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
//
//        String googleId = oidcUser.getSubject();
//        String email = oidcUser.getEmail();
//        String name = oidcUser.getFullName();
//
//        data.put("googleId", googleId);
//        data.put("email", email);
//        data.put("name", name);
//        return ResponseEntity.ok(data);
//    }

//    @GetMapping("/data")
//    public ResponseEntity<Map<String, String>> loggedInUserData(Authentication authentication) {
//        if (authentication == null)
//            return ResponseEntity.badRequest().build();
//        Map<String, String> data = new HashMap<>();
//        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
//
//        String providerId = oidcUser.getSubject();
//        String email = oidcUser.getEmail();
//        String fullName = oidcUser.getFullName();
//        String userName = oidcUser.getEmail(); // Assuming username is the email
//        String role = UserRole.ROLE_USER.name(); // Default role
//
//        data.put("providerId", providerId);
//        data.put("email", email);
//        data.put("fullName", fullName);
//        data.put("userName", userName);
//        data.put("role", role);
//        return ResponseEntity.ok(data);
//    }
}
