package org.kruskopf.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PATCH /api/users/{id}} — the self-service profile update.
 *
 * <p>Only the display name is updatable from here. {@code userName} and
 * {@code email} are declared so that an attempt to change them is <em>refused</em>
 * with an explicit 400 instead of being silently ignored:
 *
 * <ul>
 *   <li>{@code email} is the OAuth account-linking key
 *       ({@code findOrCreateOAuthUser} matches by it) and the email-whitelist key,
 *       so it must not be self-service — it needs a verified flow, or an
 *       administrator.</li>
 *   <li>{@code userName} is the form-login identity.</li>
 * </ul>
 *
 * Properties outside this record are ignored, as everywhere else in this API.
 */
public record UserProfileUpdateDTO(
        @NotBlank(message = "A display name is required")
        @Size(max = 50, message = "A display name may be at most 50 characters")
        String displayName,

        String userName,
        String email
) {
}