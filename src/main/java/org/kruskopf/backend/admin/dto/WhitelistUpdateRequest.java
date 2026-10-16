package org.kruskopf.backend.admin.dto;

/**
 * Partial update of a whitelist row — a {@code null} field is left unchanged.
 *
 * @param role   new role, or {@code null}
 * @param active {@code false} revokes the address without deleting the row,
 *               {@code true} re-admits it
 */
public record WhitelistUpdateRequest(String role, Boolean active) {
}