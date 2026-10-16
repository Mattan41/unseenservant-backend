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
import java.util.Optional;

@Service
public class UserService {

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
        // 1. Returning user for this exact provider identity.
        Optional<User> byProvider = userRepository.findByProviderTypeAndProviderId(providerType, providerId);
        if (byProvider.isPresent()) {
            return byProvider.get();
        }

        // 2. Same email already registered via another provider: reuse that account as-is.
        //    Do NOT overwrite providerId, providerType, role, userName or displayName.
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            return byEmail.get();
        }

        // 3. First time we see this person: create a user.
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

    // (Display name update) — the self-service PATCH. Narrow by design: see
    // UserProfileUpdateDTO for why the email and username are not updatable here.
    public Optional<User> updateDisplayName(long id, String displayName) {
        Optional<User> existingUserOpt = userRepository.findById(id);
        if (existingUserOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = existingUserOpt.get();
        String normalized = displayName.trim();

        if (userRepository.existsByDisplayNameAndIdNot(normalized, user.getId())) {
            throw new UniqueConstraintViolationException("displayName", normalized);
        }

        user.setDisplayName(normalized);
        return Optional.of(userRepository.save(user));
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

    /** Every account, for the admin users table. */
    public List<UserDTO> findAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::fromUser)
                .toList();
    }

    /**
     * Sets an account's role.
     * <p>
     * Deliberately separate from {@link #partialUpdate(long, Map)}: that path is
     * the self-service PATCH and must never accept a role, while this one is only
     * reachable through the admin surface, where {@code AdminService} applies the
     * self-demotion and last-administrator guards.
     */
    public User updateRole(long id, UserRole role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setRole(role);
        return userRepository.save(user);
    }

    public UserDTO findUserDTOById(long id) {
        return userRepository.findById(id)
                .map(this::toDTO) // convert User to UserDTO
                .orElse(null);
    }

}
