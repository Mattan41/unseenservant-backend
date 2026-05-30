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
    public void run(String... args) {
        // Create users
        createUserIfNotExists("User4", "User Four", "user4@example.com", "123456", UserRole.USER, ProviderType.GOOGLE);
        createUserIfNotExists("User5", "User Five", "user5@example.com", "654321", UserRole.USER, ProviderType.GOOGLE);
        createUserIfNotExists("User1", "User One", "user1@example.com", "111111", UserRole.USER, ProviderType.GOOGLE);
        createUserIfNotExists("User2", "User Two", "user2@example.com", "222222", UserRole.USER, ProviderType.GOOGLE);
        createUserIfNotExists("User3", "User Three", "user3@example.com", "333333", UserRole.USER, ProviderType.GOOGLE);
        createUserIfNotExists("admin", "Admin", "admin@admin.se", "000000", UserRole.ADMIN, ProviderType.GITHUB);


        // Create and save campaigns
        if (campaignService.getAllCampaignsRaw().isEmpty()) {

            // Retrieve users
            User user1 = userService.findByUserName("User4");
            User user2 = userService.findByUserName("User5");
            User user3 = userService.findByUserName("User1");
            User user4 = userService.findByUserName("User2");

            Campaign campaign1 = getCampaign1(user1);
            Campaign campaign2 = getCampaign2(user2);
            Campaign campaign3 = getCampaign3(user2);


            // Create and add participants
            campaign1.getParticipants().add(new CampaignUser(campaign1, user1, CampaignRole.GM, "User1"));
            campaign1.getParticipants().add(new CampaignUser(campaign1, user2, CampaignRole.PLAYER, "user2"));
            campaign1.getParticipants().add(new CampaignUser(campaign1, user3, CampaignRole.PLAYER, "User3"));
            campaign1.getParticipants().add(new CampaignUser(campaign1, user4, CampaignRole.PLAYER, "User4"));

            campaign2.getParticipants().add(new CampaignUser(campaign2, user1, CampaignRole.PLAYER, "User1"));
            campaign2.getParticipants().add(new CampaignUser(campaign2, user2, CampaignRole.GM, "User2"));
            campaign2.getParticipants().add(new CampaignUser(campaign2, user3, CampaignRole.PLAYER, "User3"));
            campaign2.getParticipants().add(new CampaignUser(campaign2, user4, CampaignRole.PLAYER, "User4"));

            campaign3.getParticipants().add(new CampaignUser(campaign3, user1, CampaignRole.PLAYER, "User1"));
            campaign3.getParticipants().add(new CampaignUser(campaign3, user2, CampaignRole.PLAYER, "User2"));
            campaign3.getParticipants().add(new CampaignUser(campaign3, user3, CampaignRole.GM, "User3"));
            campaign3.getParticipants().add(new CampaignUser(campaign3, user4, CampaignRole.PLAYER, "User4"));

            campaignService.createCampaignRaw(campaign1);
            campaignService.createCampaignRaw(campaign2);
            campaignService.createCampaignRaw(campaign3);
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
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(1L, 1L, "Gandalf", 10, "Wizard", null, "Human", new PlayerCharacterStats(11, 11, 11, 11, 15, 13)));
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(2L, 1L, "Frodo", 8, "Rogue",null, "Halfling", new PlayerCharacterStats(10, 18, 12, 12, 13, 10)));
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(3L, 1L, "Galadriel", 10, "Cleric",null, "Elf", new PlayerCharacterStats(10, 10, 10, 14, 18, 16)));
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(4L, 1L, "Gimli", 8, "Fighter",null, "Dwarf", new PlayerCharacterStats(19, 8, 16, 9, 11, 8)));

            // Character for campaign 2
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(1L, 2L, "Rincewind", 1, "Wizard",null, "Human", new PlayerCharacterStats(9, 9, 9, 14, 9, 9)));
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(2L, 2L, "Twoflower", 1, "Rogue", null,"Halfling", new PlayerCharacterStats(9, 14, 9, 9, 9, 14)));
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(3L, 2L, "Mightily Oats", 1, "Cleric",null, "Human", new PlayerCharacterStats(10, 10, 10, 10, 16, 10)));
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(4L, 2L, "Carrot Ironfoundersson", 1, "Fighter", null,"Dwarf", new PlayerCharacterStats(16, 10, 14, 10, 14, 12)));

            // Characters for campaign 3
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(1L, 3L, "Ironclad", 1, "Paladin", null,"Dragonborn", new PlayerCharacterStats(10, 10, 10, 10, 10, 10)));
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(2L, 3L, "Gearspark", 1, "Rogue", null,"Gnome", new PlayerCharacterStats(10, 10, 10, 10, 10, 10)));
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(3L, 3L, "Steamwhistle", 1, "Bard", null,"Halfling", new PlayerCharacterStats(10, 10, 10, 10, 10, 10)));
            playerCharacterService.createCharacterFromDto(new PlayerCharacterInputDTO(4L, 3L, "Cogsworth", 1, "Fighter", null,"Human", new PlayerCharacterStats(10, 10, 10, 10, 10, 10)));
        }


    }

    private static Campaign getCampaign3(User user2) {
        Campaign campaign3 = new Campaign("Campaign 3",
                """
                        In the age of steam and gears, the Iron Colossus cuts through rugged mountain passes and
                        sprawling industrial cities?an unstoppable marvel of clockwork engineering. Tonight, its
                        armored compartments carry not just passengers and freight, but a mysterious cargo rumored
                        to change the fate of the Empire. As the city?s skyline vanishes in a cloud of smoke,
                        your crew boards the train with one goal: seize the prize before rivals, lawmen,
                        or deadly automata claim it for themselves.
                        
                        Whistles scream, pistons thunder, and the race is on across perilous trestle bridges and volatile territories.
                        Every car holds secrets? cunning adversaries, exotic machinery, and deadly traps.
                        Will you outsmart the authorities, outfight mercenaries, and outpace the opposition,
                        or will the Iron Colossus become your tomb beneath the relentless steam and steel?
                        """);
        campaign3.setCreatedBy("User5");
        campaign3.setLastModifiedBy("User5");
        campaign3.setOwner(user2);
        campaign3.setImageUrl("https://slyflourish.com/images/eberron_warforged.jpg");
        return campaign3;
    }

    private static Campaign getCampaign2(User user2) {
        Campaign campaign2 = new Campaign("Neptunus", """
                 The University of Lundenwic has been robbed of its prized artifact, the Amulet of Taharka. The players are hired to track down the thief and retrieve the amulet.
                
                 Mystery & Investigation: Dive deep into a campaign filled with deception, riddles, and puzzles. Finding the thief will require sharp minds and keen eyes for detail.
                 Urban Adventure: Explore a vibrant city teeming with intrigue, hidden societies, eccentric professors, and shifting alliances.
                 Roleplay & Social Encounters: Forge alliances, interrogate suspects, and navigate the political landscape of scholars, nobles, and underground factions.
                 Skill-Based Challenges: Success will depend not just on combat prowess, but also on investigation, stealth, persuasion, and strategy.
                 Dynamic Consequences: Choices matter?a trusting word or a misstep could change the course of your investigation, and the fate of Lundenwic itself.
                """);
        campaign2.setCreatedBy("User5");
        campaign2.setLastModifiedBy("User5");
        campaign2.setOwner(user2);
        campaign2.setImageUrl("https://c4.wallpaperflare.com/wallpaper/925/634/481/league-of-legends-bilgewater-fantasy-art-pirates-wallpaper-preview.jpg");
        return campaign2;
    }

    private static Campaign getCampaign1(User user1) {
        Campaign campaign1 = new Campaign("Curse of Strahd", """
                 Under the light of the full moon, the small town of Barovia is plagued by the evil forces of Count Strahd von Zarovich. Shadows twist through cobblestone streets and forgotten forests, as villagers lock their doors and whisper prayers against the night. Ancient secrets stir beneath the mist, hungry for the return of past glories. The cries of the oppressed echo through the valleys, while Strahd?s undead minions roam freely, enforcing the will of their lord. Hope is a rare commodity in Barovia, and the line between friend and foe is often blurred. Only the bravest souls dare to stand against the darkness and confront the mysteries it conceals.
                
                 What Players Can Expect:
                
                 Dark Gothic Horror: Prepare for a campaign drenched in atmosphere, fear, and suspense. Expect themes of dread, moral ambiguity, and difficult choices as you navigate a cursed land.
                 Rich Storytelling: The narrative will focus heavily on character-driven plots, personal backstory integration, and meaningful interactions with both allies and enemies.
                 Challenging Encounters: Combat will range from desperate skirmishes against undead hordes to high-stakes battles with cunning adversaries. Prepare to use both wit and steel.
                 Investigation & Problem Solving: Mysteries abound in Barovia, from cryptic prophecies to hidden motives. Players will need to question, explore, and think creatively.
                 Roleplay Opportunities: Interact with a variety of unique NPCs, each with their own agendas and secrets. Your decisions will shape the fate of Barovia.
                 Table Guidelines:
                
                 Respect & Inclusivity: This campaign is a safe space for everyone. Discrimination, harassment, and exclusion will not be tolerated. Respect each other?s boundaries and perspectives.
                 Session Pacing: We aim for a balance between action, exploration, and roleplay. Voice any preferences to ensure everyone has fun.
                 Communication: If you?re ever uncomfortable with a scene, mechanic, or topic, please let the DM know?either in-game or privately. We use the X-card system for safety.
                 Punctuality: Please arrive on time and notify the group if you?ll be late or absent.
                 Game Etiquette: Pay attention when it?s not your turn, avoid distracting side conversations, and minimize phone use during sessions.
                 Have Fun: Remember, the goal is collective storytelling and enjoyment. Support your fellow players and embrace the horror-adventure together!
                """);
        campaign1.setCreatedBy("User4");
        campaign1.setLastModifiedBy("User4");
        campaign1.setOwner(user1);
        campaign1.setImageUrl("https://www.dndbeyond.com/attachments/8/220/cos-cover-4k.jpg");
        return campaign1;
    }

    private void createUserIfNotExists(String userName, String fullName, String email, String providerId, UserRole role, ProviderType providerType) {
        if (userService.findByUserName(userName) == null) {
            User user = new User();
            user.setUserName(userName);
            user.setPassword("password");
            user.setFullName(fullName);
            user.setEmail(email);
            user.setRole(role);
            user.setProviderType(providerType);
            user.setProviderId(providerId);
            userService.save(user);
        }
    }
}