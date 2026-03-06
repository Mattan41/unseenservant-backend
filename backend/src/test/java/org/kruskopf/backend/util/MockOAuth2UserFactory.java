package org.kruskopf.backend.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating mock OAuth2User objects for testing OAuth2 authentication.
 * <p>
 * Simulates OAuth2 responses from Google and GitHub with realistic attributes.
 */
public class MockOAuth2UserFactory {

    /**
     * Creates a mock Google OAuth2User with standard Google attributes.
     *
     * @param email User email address
     * @param name  User full name
     * @param sub   Google's unique identifier (subject)
     * @return Mock OAuth2User configured as Google user
     */
    public static OAuth2User createGoogleUser(String email, String name, String sub) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", sub);
        attributes.put("email", email);
        attributes.put("name", name);
        attributes.put("given_name", name.split(" ")[0]);
        attributes.put("family_name", name.contains(" ") ? name.split(" ")[1] : "");
        attributes.put("picture", "https://lh3.googleusercontent.com/test");
        attributes.put("email_verified", true);
        attributes.put("locale", "en");

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("SCOPE_email"),
                new SimpleGrantedAuthority("SCOPE_profile")
        );

        return new DefaultOAuth2User(authorities, attributes, "sub");
    }

    /**
     * Creates a mock GitHub OAuth2User with standard GitHub attributes.
     *
     * @param email User email (can be null, GitHub doesn't always provide it)
     * @param login GitHub username
     * @param id    GitHub unique identifier
     * @return Mock OAuth2User configured as GitHub user
     */
    public static OAuth2User createGitHubUser(String email, String login, Integer id) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", id);
        attributes.put("login", login);
        attributes.put("email", email);
        attributes.put("name", login);
        attributes.put("avatar_url", "https://avatars.githubusercontent.com/u/" + id);
        attributes.put("html_url", "https://github.com/" + login);
        attributes.put("type", "User");

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("SCOPE_read:user"),
                new SimpleGrantedAuthority("SCOPE_user:email")
        );

        return new DefaultOAuth2User(authorities, attributes, "id");
    }

    // ===================================================================
    // Convenience methods for common test scenarios
    // ===================================================================

    /**
     * Creates a Google user with whitelisted email (user1@test.com).
     */
    public static OAuth2User createWhitelistedGoogleUser() {
        return createGoogleUser("user1@test.com", "Test User One", "google-123456");
    }

    /**
     * Creates a Google user with non-whitelisted email.
     */
    public static OAuth2User createNonWhitelistedGoogleUser() {
        return createGoogleUser("notwhitelisted@example.com", "Unwanted User", "google-999999");
    }

    /**
     * Creates a Google admin user (admin@test.com).
     */
    public static OAuth2User createAdminGoogleUser() {
        return createGoogleUser("admin@test.com", "Admin User", "google-admin-123");
    }

    /**
     * Creates a GitHub user with whitelisted email (user2@test.com).
     */
    public static OAuth2User createWhitelistedGitHubUser() {
        return createGitHubUser("user2@test.com", "testuser", 12345);
    }

    /**
     * Creates a GitHub user with non-whitelisted email.
     */
    public static OAuth2User createNonWhitelistedGitHubUser() {
        return createGitHubUser("unwanted@example.com", "unwanteduser", 99999);
    }
}