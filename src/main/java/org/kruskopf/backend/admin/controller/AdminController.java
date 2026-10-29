package org.kruskopf.backend.admin.controller;

import org.kruskopf.backend.admin.dto.RoleUpdateRequest;
import org.kruskopf.backend.admin.dto.WhitelistEntryDTO;
import org.kruskopf.backend.admin.dto.WhitelistEntryRequest;
import org.kruskopf.backend.admin.dto.WhitelistUpdateRequest;
import org.kruskopf.backend.admin.service.AdminService;
import org.kruskopf.backend.audit.AuditService;
import org.kruskopf.backend.audit.dto.AuditEventDTO;
import org.kruskopf.backend.audit.entity.AuditEventType;
import org.kruskopf.backend.user.CustomUserDetails;
import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.entity.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administrator-only endpoints: the account list (role management), the email
 * whitelist that gates OAuth logins, and the audit trail those changes write to.
 * <p>
 * The whole surface is admin-only twice over: {@code /api/admin/**} is matched in
 * {@code SecurityConfig}, and the class-level {@code @PreAuthorize} repeats the
 * rule so a change to either one cannot quietly open it up.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;

    public AdminController(AdminService adminService, AuditService auditService) {
        this.adminService = adminService;
        this.auditService = auditService;
    }

    /** Every account, with its current role. */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> listUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    /** Grant or revoke administrator rights. Refuses self-demotion and the last admin. */
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserDTO> changeUserRole(
            @PathVariable Long id,
            @RequestBody RoleUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        UserRole role = AdminService.parseRole(request.role());
        return ResponseEntity.ok(adminService.changeUserRole(id, role, currentUser.user()));
    }

    /** Every whitelist row, active and revoked. */
    @GetMapping("/whitelist")
    public ResponseEntity<List<WhitelistEntryDTO>> listWhitelist() {
        return ResponseEntity.ok(adminService.listWhitelist());
    }

    /** Admit an address (or reactivate a previously revoked one). */
    @PostMapping("/whitelist")
    public ResponseEntity<WhitelistEntryDTO> addWhitelist(
            @RequestBody WhitelistEntryRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        WhitelistEntryDTO entry = adminService.addWhitelist(
                currentUser.user(), request.email(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    /** Re-role and/or revoke (or re-admit) an address. */
    @PatchMapping("/whitelist/{id}")
    public ResponseEntity<WhitelistEntryDTO> updateWhitelist(
            @PathVariable Long id,
            @RequestBody WhitelistUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        WhitelistEntryDTO entry = adminService.updateWhitelist(
                currentUser.user(), id, request.role(), request.active());
        return ResponseEntity.ok(entry);
    }

    /** Revoke an address (soft delete: the row is kept, logins are refused). */
    @DeleteMapping("/whitelist/{id}")
    public ResponseEntity<Void> removeWhitelist(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        adminService.removeWhitelist(currentUser.user(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * The audit trail: who created an account, who signed in, who changed a role
     * and who changed the whitelist — newest first.
     * <p>
     * Read-only, and there is no endpoint that writes, edits or deletes a row:
     * the trail is append-only, and its retention period
     * ({@code app.audit.retention-days}) is the only thing that ever removes one.
     *
     * @param type     only this event type; an unknown name is a 400 naming the
     *                 valid ones, rather than a filter that silently matches
     *                 nothing
     * @param beforeId the id of the last row of the page before — the cursor
     * @param limit    rows per page; clamped to {@code app.audit.max-page-size}
     */
    @GetMapping("/logs")
    public ResponseEntity<List<AuditEventDTO>> listLogs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(required = false) Integer limit) {

        AuditEventType parsedType = type == null ? null : AuditService.parseType(type);
        return ResponseEntity.ok(auditService.recent(parsedType, beforeId, limit));
    }
}