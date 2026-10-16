package org.kruskopf.backend.admin.dto;

/**
 * Request body for adding an address to the whitelist.
 *
 * @param email the address to admit (stored lower-cased)
 * @param role  {@code USER} or {@code ADMIN}
 */
public record WhitelistEntryRequest(String email, String role) {
}