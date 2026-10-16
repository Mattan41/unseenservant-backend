package org.kruskopf.backend.whitelist;

import org.kruskopf.backend.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailWhitelistRepository extends JpaRepository<EmailWhitelist, Long> {

    /**
     * Active lookup used by the OAuth login path. Case-insensitive because a
     * whitelist row is typed by hand while the provider supplies the address:
     * "User@Example.com" must admit "user@example.com".
     */
    Optional<EmailWhitelist> findByEmailIgnoreCaseAndActiveTrue(String email);

    boolean existsByEmailIgnoreCaseAndActiveTrue(String email);

    /** Any row with this address, active or not (used to reactivate a soft-deleted one). */
    Optional<EmailWhitelist> findByEmailIgnoreCase(String email);

    List<EmailWhitelist> findByRoleAndActiveTrue(UserRole role);

    @Query("SELECT e.email FROM EmailWhitelist e WHERE e.role = :role AND e.active = true")
    List<String> findEmailsByRoleAndActiveTrue(UserRole role);

    /** The admin whitelist table: every row, newest last. */
    List<EmailWhitelist> findAllByOrderByIdAsc();
}
