package org.kruskopf.backend.whitelist;

import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.user.entity.UserRole;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class EmailWhitelistService {

    private final EmailWhitelistRepository whitelistRepository;

    public EmailWhitelistService(EmailWhitelistRepository whitelistRepository) {
        this.whitelistRepository = whitelistRepository;
    }

    public boolean isEmailWhitelisted(String email) {
        return whitelistRepository.existsByEmailIgnoreCaseAndActiveTrue(email);
    }

    public Optional<UserRole> getEmailRole(String email) {
        return whitelistRepository.findByEmailIgnoreCaseAndActiveTrue(email)
                .map(EmailWhitelist::getRole);
    }

    public List<String> getUserEmails() {
        return whitelistRepository.findEmailsByRoleAndActiveTrue(UserRole.USER);
    }

    public List<String> getAdminEmails() {
        return whitelistRepository.findEmailsByRoleAndActiveTrue(UserRole.ADMIN);
    }

    /** Every row, active or not, for the admin whitelist table. */
    public List<EmailWhitelist> getAll() {
        return whitelistRepository.findAllByOrderByIdAsc();
    }

    /**
     * Adds an address to the whitelist, or reactivates and re-roles the row that
     * already exists for it. The email column is unique and removal is a soft
     * delete, so a plain insert would violate the constraint.
     */
    public EmailWhitelist addOrReactivateEmail(String email, UserRole role) {
        String normalizedEmail = normalizeEmail(email);
        return whitelistRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(existing -> {
                    existing.setRole(role);
                    existing.setActive(true);
                    return whitelistRepository.save(existing);
                })
                .orElseGet(() -> whitelistRepository.save(new EmailWhitelist(normalizedEmail, role)));
    }

    /**
     * Applies a partial update to one row. A {@code null} argument is left as it
     * is, so the caller can change the role, the active flag, or both.
     */
    public EmailWhitelist updateEmail(Long id, UserRole role, Boolean active) {
        EmailWhitelist entry = findById(id);
        if (role != null) {
            entry.setRole(role);
        }
        if (active != null) {
            entry.setActive(active);
        }
        return whitelistRepository.save(entry);
    }

    /**
     * Soft delete: the row stays for the audit trail, but stops admitting logins.
     *
     * @return the row as it was left — {@code active = false}, and the address the
     *   audit trail names
     */
    public EmailWhitelist removeEmailById(Long id) {
        return updateEmail(id, null, false);
    }

    public void removeEmailFromWhitelist(String email) {
        whitelistRepository.findByEmailIgnoreCaseAndActiveTrue(email)
                .ifPresent(emailWhitelist -> {
                    emailWhitelist.setActive(false);
                    whitelistRepository.save(emailWhitelist);
                });
    }

    /**
     * Keeps the stored login role in step with an account whose role an
     * administrator just changed. Only active rows are touched — a revoked
     * address must not be re-admitted by a role change.
     *
     * @return {@code true} when an active row was updated
     */
    public boolean syncRoleForEmail(String email, UserRole role) {
        return whitelistRepository.findByEmailIgnoreCaseAndActiveTrue(email)
                .map(entry -> {
                    entry.setRole(role);
                    whitelistRepository.save(entry);
                    return true;
                })
                .orElse(false);
    }

    /** Addresses are stored lower-cased; providers and admins do not agree on casing. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private EmailWhitelist findById(Long id) {
        return whitelistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No whitelist entry with id: " + id));
    }
}

