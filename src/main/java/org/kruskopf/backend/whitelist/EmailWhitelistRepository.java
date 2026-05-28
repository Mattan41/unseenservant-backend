package org.kruskopf.backend.whitelist;

import org.kruskopf.backend.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailWhitelistRepository extends JpaRepository<EmailWhitelist, Long> {

    Optional<EmailWhitelist> findByEmailAndActiveTrue(String email);

    List<EmailWhitelist> findByRoleAndActiveTrue(UserRole role);

    @Query("SELECT e.email FROM EmailWhitelist e WHERE e.role = :role AND e.active = true")
    List<String> findEmailsByRoleAndActiveTrue(UserRole role);

    boolean existsByEmailAndActiveTrue(String email);
}
