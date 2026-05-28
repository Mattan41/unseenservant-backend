package org.kruskopf.backend.whitelist;

import org.kruskopf.backend.user.entity.UserRole;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmailWhitelistService {

    private final EmailWhitelistRepository whitelistRepository;

    public EmailWhitelistService(EmailWhitelistRepository whitelistRepository) {
        this.whitelistRepository = whitelistRepository;
    }

    public boolean isEmailWhitelisted(String email) {
        return whitelistRepository.existsByEmailAndActiveTrue(email);
    }

    public Optional<UserRole> getEmailRole(String email) {
        return whitelistRepository.findByEmailAndActiveTrue(email)
                .map(EmailWhitelist::getRole);
    }

    public List<String> getUserEmails() {
        return whitelistRepository.findEmailsByRoleAndActiveTrue(UserRole.USER);
    }

    public List<String> getAdminEmails() {
        return whitelistRepository.findEmailsByRoleAndActiveTrue(UserRole.ADMIN);
    }

    public EmailWhitelist addEmailToWhitelist(String email, UserRole role) {
        EmailWhitelist whitelist = new EmailWhitelist(email, role);
        return whitelistRepository.save(whitelist);
    }

    public void removeEmailFromWhitelist(String email) {
        whitelistRepository.findByEmailAndActiveTrue(email)
                .ifPresent(emailWhitelist -> {
                    emailWhitelist.setActive(false);
                    whitelistRepository.save(emailWhitelist);
                });
    }
}

