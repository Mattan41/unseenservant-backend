package org.kruskopf.backend.admin.service;

import org.kruskopf.backend.admin.dto.WhitelistEntryDTO;
import org.kruskopf.backend.audit.AuditService;
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
import java.util.Objects;
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
    private final AuditService auditService;

    public AdminService(UserService userService,
                        UserRepository userRepository,
                        EmailWhitelistService emailWhitelistService,
                        AuditService auditService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.emailWhitelistService = emailWhitelistService;
        this.auditService = auditService;
    }

    /** Every account, for the users table. */
    @Transactional(readOnly = true)
    public List<UserDTO> listUsers() {
        return userService.findAllUsers();
    }

    /**
     * Changes an account's role and keeps its whitelist row in step.
     * <p>
     * The change is audited in this transaction ({@code AuditService.roleChanged}),
     * so a trail that cannot be written fails the change with it. The
     * self-demotion refusal is deliberately <em>not</em> recorded: it is answered
     * before the lookup, changes nothing, and the administrator who tried it is
     * told. The last-administrator refusal is recorded, because that one says
     * somebody is about to empty the administrator seat.
     *
     * @param targetUserId the account to change
     * @param role         the new role
     * @param actor        the administrator making the change, for the audit row
     */
    @Transactional
    public UserDTO changeUserRole(long targetUserId, UserRole role, User actor) {
        if (Objects.equals(actor.getId(), targetUserId)) {
            throw new InvalidRequestException("You cannot change your own role.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id: " + targetUserId));

        boolean demotion = target.getRole() == UserRole.ADMIN && role != UserRole.ADMIN;
        if (demotion && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            auditService.roleChangeRefused(actor, target, "the last administrator cannot be demoted");
            throw new InvalidRequestException("The last administrator cannot be demoted.");
        }

        UserRole previousRole = target.getRole();
        User updated = userService.updateRole(targetUserId, role);
        emailWhitelistService.syncRoleForEmail(updated.getEmail(), role);
        auditService.roleChanged(actor, updated, previousRole, role);
        return userService.toDTO(updated);
    }

    /** Every whitelist row, active or not. */
    @Transactional(readOnly = true)
    public List<WhitelistEntryDTO> listWhitelist() {
        return emailWhitelistService.getAll().stream()
                .map(WhitelistEntryDTO::from)
                .toList();
    }

    /**
     * Admits an address, or reactivates the row that already exists for it.
     * Audited in this transaction, with the address as stored (lower-cased).
     */
    @Transactional
    public WhitelistEntryDTO addWhitelist(User actor, String email, String role) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new InvalidRequestException("A valid email address is required.");
        }
        EmailWhitelist entry = emailWhitelistService.addOrReactivateEmail(email, parseRole(role));
        auditService.whitelistAdded(actor, entry.getEmail(), entry.getRole());
        return WhitelistEntryDTO.from(entry);
    }

    /**
     * Partial update: re-roles and/or revokes (or re-admits) one address.
     * Audited in this transaction; the row's state after the change is what the
     * trail names, so the log reads as the outcome rather than the request.
     */
    @Transactional
    public WhitelistEntryDTO updateWhitelist(User actor, Long id, String role, Boolean active) {
        if (role == null && active == null) {
            throw new InvalidRequestException("Nothing to update: provide a role, an active flag, or both.");
        }
        UserRole parsedRole = role == null ? null : parseRole(role);
        EmailWhitelist entry = emailWhitelistService.updateEmail(id, parsedRole, active);
        auditService.whitelistUpdated(actor, entry.getEmail(), entry.getRole(), entry.isActive());
        return WhitelistEntryDTO.from(entry);
    }

    /** Soft delete — the row is kept for the audit trail but stops admitting logins. */
    @Transactional
    public void removeWhitelist(User actor, Long id) {
        EmailWhitelist entry = emailWhitelistService.removeEmailById(id);
        auditService.whitelistRevoked(actor, entry.getEmail());
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