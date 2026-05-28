package org.kruskopf.backend.config;


import org.kruskopf.backend.whitelist.EmailWhitelist;
import org.kruskopf.backend.whitelist.EmailWhitelistRepository;
import org.kruskopf.backend.user.entity.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;


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
        // Lägg till initial whitelist-data om tabellen är tom
        if (whitelistRepository.count() == 0) {
            // Admins
            adminWhitelist.forEach(email ->
                    whitelistRepository.save(new EmailWhitelist(email, UserRole.ADMIN))
            );

            // Users
            userWhitelist.forEach(email ->
                    whitelistRepository.save(new EmailWhitelist(email, UserRole.USER))
            );
        }
    }
}