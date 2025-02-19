package org.kruskopf.backend.user.service;

import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static org.kruskopf.backend.user.UserRole.ROLE_ADMIN;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User find(String id) {
        Optional<User> user = userRepository.findByProviderId(id);
        return user.orElseThrow(() -> new RuntimeException("No such ID " + id));

    }

    public User findByUserName(String userName) {
        return userRepository.findByUserName(userName).orElse(null);
    }

    public User loadByUserName(String userName) {
        return userRepository.findByUserName(userName).orElseThrow(() -> new RuntimeException("User not found: " + userName));
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User createAdminUser(String userName, String email, String password, String providerId, ProviderType providerType) {
        User admin = new User();
        admin.setProviderId(providerId);
        admin.setProviderType(providerType);
        admin.setUserName(userName);
        admin.setEmail(email);
        admin.setPassword(password);
        admin.setRole(ROLE_ADMIN);
        return userRepository.save(admin);
    }

    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}