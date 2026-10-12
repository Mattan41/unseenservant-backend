package org.kruskopf.backend.user.service;

import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserServiceOAuthLoginIT extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void newOAuthLoginCreatesAUser() {
        long before = userRepository.count();

        User user = userService.findOrCreateOAuthUser(
                "google-sub-new", ProviderType.GOOGLE, "oauth-new@example.com", "New OAuth", UserRole.USER);

        assertNotNull(user.getId());
        assertEquals("oauth-new@example.com", user.getEmail());
        assertEquals(ProviderType.GOOGLE, user.getProviderType());
        assertEquals("google-sub-new", user.getProviderId());
        assertEquals(before + 1, userRepository.count());
    }

    @Test
    void sameProviderTwiceReturnsTheSameUser() {
        User first = userService.findOrCreateOAuthUser(
                "google-sub-same", ProviderType.GOOGLE, "oauth-same@example.com", "Same Provider", UserRole.USER);
        long afterFirst = userRepository.count();

        User second = userService.findOrCreateOAuthUser(
                "google-sub-same", ProviderType.GOOGLE, "oauth-same@example.com", "Same Provider", UserRole.USER);

        assertEquals(first.getId(), second.getId());
        assertEquals(afterFirst, userRepository.count());
    }

    @Test
    void sameEmailViaOtherProviderReturnsTheSameUserAndCreatesNoNewRow() {
        User googleUser = userService.findOrCreateOAuthUser(
                "google-sub-shared", ProviderType.GOOGLE, "oauth-shared@example.com", "Shared Email", UserRole.USER);
        long afterFirst = userRepository.count();

        User githubUser = userService.findOrCreateOAuthUser(
                "github-id-shared", ProviderType.GITHUB, "oauth-shared@example.com", "Shared Email", UserRole.USER);

        assertEquals(googleUser.getId(), githubUser.getId());
        assertEquals(afterFirst, userRepository.count());

        // The existing account is returned unchanged (provider identity is not overwritten).
        User reloaded = userRepository.findById(googleUser.getId()).orElseThrow();
        assertEquals(ProviderType.GOOGLE, reloaded.getProviderType());
        assertEquals("google-sub-shared", reloaded.getProviderId());
    }
}
