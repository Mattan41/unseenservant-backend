package org.kruskopf.backend.admin.dto;

/**
 * Request body for changing an account's role.
 *
 * @param role {@code USER} or {@code ADMIN}
 */
public record RoleUpdateRequest(String role) {
}