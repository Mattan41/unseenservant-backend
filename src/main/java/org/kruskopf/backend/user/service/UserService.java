package org.kruskopf.backend.user.service;

import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UniqueConstraintViolationException;
import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    // TODO: use DTOs for more narrow updates, use Preauthorizate

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDTO> searchUsers(String query, long currentUserId) {
        List<User> users = userRepository.findByUserNameContainingOrEmailContainingOrFullNameContaining(
                query, query, query);

        return users.stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .map(UserDTO::fromUser)
                .toList();
    }

    public User findOrCreateOAuthUser(String providerId, ProviderType providerType,
                                      String email, String fullName, UserRole role) {
        return userRepository.findByProviderId(providerId)
                .orElseGet(() -> {
                    // Generate a random and secure pwd-string for OAuth2-users
                    String securePlaceholder = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
                    User newUser = new User(
                            providerId,
                            providerType,
                            email,
                            fullName,
                            email, // username = email as default for OAuth
                            role,
                            securePlaceholder
                    );
                    return userRepository.save(newUser);
                });
    }

    public Optional<User> findById(long id) {
        return userRepository.findById(id);
    }

    public User find(String id) {
        Optional<User> user = userRepository.findByProviderId(id);
        return user.orElseThrow(() -> new ResourceNotFoundException("No such ID " + id));
    }

    public Optional<User> findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    public User loadByUserName(String userName) {
        return userRepository.findByUserName(userName).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userName));
    }

    // (Partial Update - PATCH) - update specific fields of a user
    public Optional<User> partialUpdate(long id, Map<String, Object> updates) {
        Optional<User> existingUserOpt = userRepository.findById(id);
        if (existingUserOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = existingUserOpt.get();

        // update the fields based on input
        updates.forEach((key, value) -> {
            switch (key) {
                case "userName" -> user.setUserName((String) value);
                case "email" -> user.setEmail((String) value);
                case "displayName" -> {
                    String newDisplayName = (String) value;
                    if (userRepository.existsByDisplayNameAndIdNot(newDisplayName, user.getId())) {
                        throw new UniqueConstraintViolationException("displayName", newDisplayName);
                    }
                    user.setDisplayName(newDisplayName);
                }

                default -> throw new IllegalArgumentException("Field " + key + " not supported for update");
            }
        });

        User updatedUser = userRepository.save(user);
        return Optional.of(updatedUser);
    }

    // Method to delete a user by ID - we could also use soft delete with a boolean field, perhaps remove this method
    public boolean deleteById(long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public UserDTO toDTO(User user) {
        return UserDTO.fromUser(user);
    }

    public UserDTO findUserDTOById(long id) {
        return userRepository.findById(id)
                .map(this::toDTO) // convert User to UserDTO
                .orElse(null);
    }

}
