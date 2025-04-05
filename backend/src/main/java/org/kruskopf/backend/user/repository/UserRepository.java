package org.kruskopf.backend.user.repository;

import org.kruskopf.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserName(String userName);

    Optional<User> findByProviderId(String providerId);

    List<User> findByUserNameContainingOrEmailContainingOrFullNameContaining(
            String username, String email, String fullName);

}
