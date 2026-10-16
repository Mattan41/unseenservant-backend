package org.kruskopf.backend.user.repository;

import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserName(String userName);

    Optional<User> findByProviderId(String providerId);

    Optional<User> findByProviderTypeAndProviderId(ProviderType providerType, String providerId);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByUserNameContainingOrEmailContainingOrFullNameContaining(
            String username, String email, String fullName);

    boolean existsByDisplayNameAndIdNot(String displayName, Long userId);

    /**
     * Used by the admin surface to refuse removing the last administrator — the
     * only account that could undo it.
     */
    long countByRole(UserRole role);

}
