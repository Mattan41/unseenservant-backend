package org.kruskopf.backend.admin.service;

import org.kruskopf.backend.admin.dto.WhitelistEntryDTO;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.kruskopf.backend.user.service.UserService;
import org.kruskopf.backend.whitelist.EmailWhitelist;
import org.kruskopf.backend.whitelist.EmailWhitelistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The administrator surface: account roles and the email whitelist.
 * <p>
 * Two rules are enforced here rather than in the controller, because both need
 * the database:
 * <ul>
 *   <li>an administrator cannot change their own role — a mis-click would revoke
 *       the session's rights immediately, since authorities are rebuilt from the
 *       database on every request;</li>
 *   <li>the last remaining administrator cannot be demoted. That branch is
 *       defence in depth: with today's DB-backed authorities the caller must
 *       itself be an administrator row, so a demotion always leaves one behind,
 *       and the self-demotion rule above is what actually protects the last
 *       account. The check stays for the day authorities are cached or taken
 *       from the JWT claim instead.</li>
 * </ul>
 * A role change also mirrors the address's active whitelist row, so a rebuilt
 * database does not silently demote the account at its next login. Note that the
 * whitelist role only decides what a <em>first</em> login is granted: an account
 * that already exists keeps its own role.
 */
@Service
public class AdminService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final EmailWhitelistService emailWhitelistService;

    public AdminService(UserService userService,
                        UserRepository userRepository,
                        EmailWhitelistService emailWhitelistService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.emailWhitelistService = emailWhitelistService;
    }

    /** Every account, for the users table. */
    @Transactional(readOnly = true)
    public List<UserDTO> listUsers() {
        return userService.findAllUsers();
    }

    /**
     * Changes an account's role and keeps its whitelist row in step.
     *
     * @param targetUserId the account to change
     * @param role         the new role
     * @param actingUserId the administrator making the change
     */
    @Transactional
    public UserDTO changeUserRole(long targetUserId, UserRole role, long actingUserId) {
        if (targetUserId == actingUserId) {
            throw new InvalidRequestException("You cannot change your own role.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id: " + targetUserId));

        boolean demotion = target.getRole() == UserRole.ADMIN && role != UserRole.ADMIN;
        if (demotion && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new InvalidRequestException("The last administrator cannot be demoted.");
        }

        User updated = userService.updateRole(targetUserId, role);
        emailWhitelistService.syncRoleForEmail(updated.getEmail(), role);
        return userService.toDTO(updated);
    }

    /** Every whitelist row, active or not. */
    @Transactional(readOnly = true)
    public List<WhitelistEntryDTO> listWhitelist() {
        return emailWhitelistService.getAll().stream()
                .map(WhitelistEntryDTO::from)
                .toList();
    }

    /** Admits an address, or reactivates the row that already exists for it. */
    @Transactional
    public WhitelistEntryDTO addWhitelist(String email, String role) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new InvalidRequestException("A valid email address is required.");
        }
        EmailWhitelist entry = emailWhitelistService.addOrReactivateEmail(email, parseRole(role));
        return WhitelistEntryDTO.from(entry);
    }

    /** Partial update: re-roles and/or revokes (or re-admits) one address. */
    @Transactional
    public WhitelistEntryDTO updateWhitelist(Long id, String role, Boolean active) {
        if (role == null && active == null) {
            throw new InvalidRequestException("Nothing to update: provide a role, an active flag, or both.");
        }
        UserRole parsedRole = role == null ? null : parseRole(role);
        EmailWhitelist entry = emailWhitelistService.updateEmail(id, parsedRole, active);
        return WhitelistEntryDTO.from(entry);
    }

    /** Soft delete — the row is kept for the audit trail but stops admitting logins. */
    @Transactional
    public void removeWhitelist(Long id) {
        emailWhitelistService.removeEmailById(id);
    }

    /**
     * Parses a role name from a request body.
     *
     * @throws InvalidRequestException when the value is missing or unknown
     */
    public static UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new InvalidRequestException("A role is required (" + roleNames() + ").");
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Unknown role '" + role + "' (" + roleNames() + ").");
        }
    }

    private static String roleNames() {
        return Arrays.stream(UserRole.values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}