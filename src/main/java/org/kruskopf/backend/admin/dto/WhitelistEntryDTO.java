package org.kruskopf.backend.admin.dto;

import org.kruskopf.backend.whitelist.EmailWhitelist;

/**
 * One row of the admin whitelist table.
 *
 * @param id     row id (used by PATCH/DELETE)
 * @param email  the address, stored lower-cased
 * @param role   the role granted on a first OAuth login with this address
 * @param active {@code false} after a soft delete — the row is kept, but stops
 *               admitting logins
 */
public record WhitelistEntryDTO(Long id, String email, String role, boolean active) {

    public static WhitelistEntryDTO from(EmailWhitelist entry) {
        return new WhitelistEntryDTO(
                entry.getId(),
                entry.getEmail(),
                entry.getRole().name(),
                entry.isActive());
    }
}