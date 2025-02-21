package org.kruskopf.backend;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.service.CampaignService;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class StartupRunner implements CommandLineRunner {


    private final UserService userService;
    private final CampaignService campaignService;

    public StartupRunner(UserService userService, CampaignService campaignService) {
        this.userService = userService;
        this.campaignService = campaignService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Create users
        createUserIfNotExists("Mattan", "password", "Mats Kruskopf", "krishopf@gmail.com", "123456");
        createUserIfNotExists("Mats", "password", "Mats Kruskopf Eriksson", "mats.fpoksurk@gmail.com", "654321");
        createUserIfNotExists("User1", "password", "User One", "user1@example.com", "111111");
        createUserIfNotExists("User2", "password", "User Two", "user2@example.com", "222222");
        createUserIfNotExists("User3", "password", "User Three", "user3@example.com", "333333");


        // Create admin user
        if (userService.findByUserName("admin") == null) {
            userService.createAdminUser("admin", "admin@admin.se", "admin", "000000", ProviderType.GOOGLE);
        }

        // Create and save campaigns
        if (campaignService.findAll().isEmpty()) {
            Campaign campaign1 = new Campaign("Campaign 1", "Description of Campaign 1");
            campaign1.setCreatedBy("Mattan");
            campaign1.setLastModifiedBy("Mattan");
            Campaign campaign2 = new Campaign("Campaign 2", "Description of Campaign 2");
            campaign2.setCreatedBy("Mats");
            campaign2.setLastModifiedBy("Mats");

            // Retrieve users
            User user1 = userService.findByUserName("Mattan");
            User user2 = userService.findByUserName("Mats");
            User user3 = userService.findByUserName("User1");
            User user4 = userService.findByUserName("User2");

            // Create and add participants
            campaign1.getParticipants().add(new CampaignUser(campaign1, user1, CampaignRole.PLAYER, "Mattan"));
            campaign1.getParticipants().add(new CampaignUser(campaign1, user2, CampaignRole.PLAYER, "Mats"));
            campaign1.getParticipants().add(new CampaignUser(campaign1, user3, CampaignRole.PLAYER, "User1"));

            campaign2.getParticipants().add(new CampaignUser(campaign2, user2, CampaignRole.PLAYER, "Mats"));
            campaign2.getParticipants().add(new CampaignUser(campaign2, user4, CampaignRole.PLAYER, "User2"));

            campaignService.save(campaign1);
            campaignService.save(campaign2);
        }
    }

    private void createUserIfNotExists(String userName, String password, String fullName, String email, String providerId) {
        if (userService.findByUserName(userName) == null) {
            User user = new User();
            user.setUserName(userName);
            user.setPassword(password);
            user.setFullName(fullName);
            user.setEmail(email);
            user.setRole(UserRole.USER);
            user.setProviderType(ProviderType.GOOGLE);
            user.setProviderId(providerId);
            userService.save(user);
        }
    }
}
