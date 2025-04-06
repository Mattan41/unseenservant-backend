package org.kruskopf.backend.user.service;

import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.kruskopf.backend.user.entity.UserRole.ADMIN;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDTO> searchUsers(String query, Long currentUserId) {
        List<User> users = userRepository.findByUserNameContainingOrEmailContainingOrFullNameContaining(
                query, query, query);

        return users.stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .map(UserDTO::fromUser)
                .toList();
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
        admin.setRole(ADMIN);
        return userRepository.save(admin);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
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

    // (Partial Update - PATCH) - update specific fields of a user
    public Optional<User> partialUpdate(Long id, Map<String, Object> updates) {
        Optional<User> existingUserOpt = userRepository.findById(id);
        if (existingUserOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = existingUserOpt.get();

        // update the fields based on input // todo review the fields here - which should user be able to update? and how to solve Admin has more rights? preauthorize? use different methods?
        updates.forEach((key, value) -> {
            switch (key) {
                case "userName" -> user.setUserName((String) value);
                case "email" -> user.setEmail((String) value);
                case "displayName" -> user.setDisplayName((String) value);
                default -> throw new IllegalArgumentException("Field " + key + " not supported for update");
            }
        });

        User updatedUser = userRepository.save(user);
        return Optional.of(updatedUser);
    }

    // Method to update a user fully by ID
    public Optional<User> update(Long id, User updatedUser) {
        if (!userRepository.existsById(id)) {
            return Optional.empty();
        }

        updatedUser.setId(id);

        User savedUser = userRepository.save(updatedUser);
        return Optional.of(savedUser);
    }

    // Method to delete a user by ID
    public boolean deleteById(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public UserDTO toDTO(User user) {
        return UserDTO.fromUser(user);
    }

    public UserDTO findUserDTOById(Long id) {
        return userRepository.findById(id)
                .map(this::toDTO) // convert User to UserDTO
                .orElse(null);
    }

}
