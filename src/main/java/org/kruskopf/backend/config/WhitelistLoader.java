package org.kruskopf.backend.config;


import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.whitelist.EmailWhitelist;
import org.kruskopf.backend.whitelist.EmailWhitelistRepository;
import org.kruskopf.backend.whitelist.EmailWhitelistService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;


@Component
public class WhitelistLoader implements CommandLineRunner {

    private final EmailWhitelistRepository whitelistRepository;

    @Value("#{'${ADMIN_WHITELIST:}'.split(',')}")
    private List<String> adminWhitelist;

    @Value("#{'${USER_WHITELIST:}'.split(',')}")
    private List<String> userWhitelist;

    public WhitelistLoader(EmailWhitelistRepository whitelistRepository) {
        this.whitelistRepository = whitelistRepository;
    }

    @Override
    public void run(String... args) {
        // Add initial whitelist data if the table is empty
        if (whitelistRepository.count() == 0) {
            // Admins
            seed(adminWhitelist, UserRole.ADMIN);

            // Users
            seed(userWhitelist, UserRole.USER);
        }
    }

    /**
     * Seeds one role's addresses.
     * <p>
     * The {@code split(',')} expression yields a single empty string when the
     * environment variable is unset, and hand-written addresses may be in any
     * case, so blanks are dropped and the rest is stored lower-cased — the same
     * normalisation the admin endpoints and the login lookup use.
     */
    private void seed(List<String> emails, UserRole role) {
        emails.stream()
                .map(EmailWhitelistService::normalizeEmail)
                .filter(Objects::nonNull)
                .filter(email -> !email.isBlank())
                .forEach(email -> whitelistRepository.save(new EmailWhitelist(email, role)));
    }
}