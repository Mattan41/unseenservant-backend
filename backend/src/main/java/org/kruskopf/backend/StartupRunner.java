package org.kruskopf.backend;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.service.CampaignService;
import org.kruskopf.backend.message.dto.MessageDTO;
import org.kruskopf.backend.message.service.MessageService;
import org.kruskopf.backend.playercharacter.PlayerCharacterStats;
import org.kruskopf.backend.playercharacter.dto.PlayerCharacterInputDTO;
import org.kruskopf.backend.playercharacter.service.PlayerCharacterService;
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
    private final MessageService messageService;
    private final PlayerCharacterService playerCharacterService;

    public StartupRunner(UserService userService, CampaignService campaignService, MessageService messageService, PlayerCharacterService playerCharacterService) {
        this.userService = userService;
        this.campaignService = campaignService;
        this.messageService = messageService;
        this.playerCharacterService = playerCharacterService;
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

        // Create and save messages
        if (messageService.getAllMessages().isEmpty()) {
            messageService.createMessage(new MessageDTO(1L, 1L, "Wow this campaign is great!"));
            messageService.createMessage(new MessageDTO(1L, 2L, "I agree! What character are you going to play?"));
            messageService.createMessage(new MessageDTO(1L, 3L, "I am going to play a wizard!"));
            messageService.createMessage(new MessageDTO(2L, 2L, "Hello, I am new to this campaign!"));
            messageService.createMessage(new MessageDTO(2L, 4L, "Welcome! Any questions?"));
        }

        // Create and save characters
        if (playerCharacterService.getAllCharacters().isEmpty()) {
            // Characters for campaign 1
            playerCharacterService.createCharacter(new PlayerCharacterInputDTO(
                    1L, 1L, "Gandalf", 10, "Wizard", "Human",
                    new PlayerCharacterStats(11, 11, 11, 11, 15, 13)
            ));
            playerCharacterService.createCharacter(new PlayerCharacterInputDTO(
                    2L, 1L, "Frodo", 8, "Rogue", "Halfling",
                    new PlayerCharacterStats(10, 18, 12, 12, 13, 10)
            ));
            playerCharacterService.createCharacter(new PlayerCharacterInputDTO(
                    3L, 1L, "Galadriel", 10, "Cleric", "Elf",
                    new PlayerCharacterStats(10, 10, 10, 14, 18, 16)
            ));
            playerCharacterService.createCharacter(new PlayerCharacterInputDTO(
                    4L, 1L, "Gimli", 8, "Fighter", "Dwarf",
                    new PlayerCharacterStats(19, 8, 16, 9, 11, 8)
            ));

            // Character for campaign 2
            playerCharacterService.createCharacter(new PlayerCharacterInputDTO(
                    1L, 2L, "Rincewind", 1, "Wizard", "Human",
                    new PlayerCharacterStats(9, 9, 9, 14, 9, 9)
            ));
            playerCharacterService.createCharacter(new PlayerCharacterInputDTO(
                    2L, 2L, "Twoflower", 1, "Rogue", "Halfling",
                    new PlayerCharacterStats(9, 14, 9, 9, 9, 14)
            ));
            playerCharacterService.createCharacter(new PlayerCharacterInputDTO(
                    3L, 2L, "Mightily Oats", 1, "Cleric", "Human",
                    new PlayerCharacterStats(10, 10, 10, 10, 16, 10)
            ));
            playerCharacterService.createCharacter(new PlayerCharacterInputDTO(
                    4L, 2L, "Carrot Ironfoundersson", 1, "Fighter", "Dwarf",
                    new PlayerCharacterStats(16, 10, 14, 10, 14, 12)
            ));
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
