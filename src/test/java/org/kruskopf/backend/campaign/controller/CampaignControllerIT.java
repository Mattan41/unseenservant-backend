package org.kruskopf.backend.campaign.controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.auth.JwtService;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;
import org.kruskopf.backend.playercharacter.repository.GameCharacterRepository;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The roster over HTTP, and what a roster change does to the character domain: deleting
 * a campaign and removing a participant both leave that member's characters with no
 * campaign. The campaign side only announces it — {@code CampaignMembershipListener} is
 * what carries it out — so these are the tests that would catch the listener being
 * unwired, an event nobody handles, or the unlink arriving after the campaign row has
 * already gone (the character's foreign key would then refuse the delete outright).
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("Campaign roster Integration Tests")
class CampaignControllerIT extends AbstractIntegrationTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignUserRepository campaignUserRepository;
    private final GameCharacterRepository gameCharacterRepository;
    private final EntityManager entityManager;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    CampaignControllerIT(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            JwtService jwtService,
            UserRepository userRepository,
            CampaignRepository campaignRepository,
            CampaignUserRepository campaignUserRepository,
            GameCharacterRepository gameCharacterRepository,
            EntityManager entityManager) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.campaignUserRepository = campaignUserRepository;
        this.gameCharacterRepository = gameCharacterRepository;
        this.entityManager = entityManager;
    }

    private User owner;
    private User gm;
    private User player;
    private User stayer;
    private Campaign campaign;
    private GameCharacter playerCharacter;
    private GameCharacter stayerCharacter;

    @BeforeEach
    void setUp() {
        // Clean up in FK-safe order
        gameCharacterRepository.deleteAll();
        campaignUserRepository.deleteAll();
        campaignRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(TestDataFactory.aUser("owner"));
        gm = userRepository.save(TestDataFactory.aUser("gm"));
        player = userRepository.save(TestDataFactory.aUser("player"));
        stayer = userRepository.save(TestDataFactory.aUser("stayer"));

        // The owner is a PLAYER deliberately: ownership and the GM role are two
        // separate axes, and a fixture that conflates them would keep passing while the
        // wrong one was being checked.
        campaign = campaignRepository.save(TestDataFactory.aCampaign(owner));

        // The roster goes in through the campaign's own collection, the way the
        // application builds it. Seeded straight into campaign_user through the
        // repository, the entity the request later loads would report an empty
        // participant list — and every removal would answer 404.
        campaign.getParticipants().add(new CampaignUser(campaign, owner, CampaignRole.PLAYER, "The Owner"));
        campaign.getParticipants().add(new CampaignUser(campaign, gm, CampaignRole.GM, "The GM"));
        campaign.getParticipants().add(new CampaignUser(campaign, player, CampaignRole.PLAYER, "Player"));
        campaign.getParticipants().add(new CampaignUser(campaign, stayer, CampaignRole.PLAYER, "Stayer"));

        playerCharacter = gameCharacterRepository.save(TestDataFactory.aCharacter(player, campaign));
        stayerCharacter = gameCharacterRepository.save(TestDataFactory.aCharacter(stayer, campaign));

        // Then hand the request an empty persistence context, as a real request would
        // have: sharing this test's session would have it read the roster and the
        // characters as they were built here instead of as the database holds them.
        entityManager.flush();
        entityManager.clear();
    }

    private ResultActions patchParticipants(User caller, String body) throws Exception {
        return mockMvc.perform(patch("/api/campaigns/" + campaign.getId() + "/participants")
                .header("Authorization", "Bearer " + jwtService.generateToken(caller))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions deleteCampaign(User caller) throws Exception {
        return mockMvc.perform(delete("/api/campaigns/" + campaign.getId())
                .header("Authorization", "Bearer " + jwtService.generateToken(caller)));
    }

    /** The removal half of the roster request, naming users by id (which is what it takes). */
    private static String removeBody(User... users) {
        List<String> ids = new ArrayList<>();
        for (User user : users) {
            ids.add(String.valueOf(user.getId()));
        }
        return "{\"participantIdsToRemove\":[" + String.join(",", ids) + "]}";
    }

    /**
     * A character as the database holds it. Flushing and clearing first is what makes
     * this an assertion: the unlink is a bulk update, so it does not touch the entity
     * sitting in the persistence context and an in-memory instance would still show the
     * campaign it no longer has.
     */
    private GameCharacter reloaded(GameCharacter character) {
        entityManager.flush();
        entityManager.clear();
        return gameCharacterRepository.findById(character.getId()).orElseThrow();
    }

    /** The campaign as the database holds it, or null once it is gone. */
    private Campaign reloaded(Campaign toReload) {
        entityManager.flush();
        entityManager.clear();
        return campaignRepository.findById(toReload.getId()).orElse(null);
    }

    /** The participant ids an answer carries, in the order it lists them. */
    private static List<Long> participantIdsOf(JsonNode answer) {
        List<Long> ids = new ArrayList<>();
        answer.get("participants").forEach(node -> ids.add(node.get("id").asLong()));
        return ids;
    }

    /** The JSON body of a request that is expected to have succeeded. */
    private JsonNode answerOf(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("DELETE /api/campaigns/{id}")
    class DeleteCampaign {

        @Test
        @DisplayName("the owner deletes it and every member's character comes back unlinked")
        void ownerDeletesTheCampaignAndUnlinksEveryMember() throws Exception {
            deleteCampaign(owner).andExpect(status().isNoContent());

            assertThat(reloaded(campaign)).isNull();
            assertThat(reloaded(playerCharacter).getCampaign()).isNull();
            assertThat(reloaded(stayerCharacter).getCampaign()).isNull();
        }

        @Test
        @DisplayName("a non-owner is refused, and nothing is unlinked")
        void nonOwnerCannotDelete() throws Exception {
            deleteCampaign(gm).andExpect(status().isForbidden());

            assertThat(reloaded(campaign)).isNotNull();
            assertThat(reloaded(playerCharacter).getCampaign()).isNotNull();
        }

        @Test
        @DisplayName("answers 401 without a token")
        void answers401WithoutAToken() throws Exception {
            mockMvc.perform(delete("/api/campaigns/" + campaign.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /api/campaigns/{id}/participants")
    class UpdateParticipants {

        @Test
        @DisplayName("removing a participant unlinks their characters and leaves the rest in the campaign")
        void removingAParticipantUnlinksTheirCharacters() throws Exception {
            JsonNode answer = answerOf(patchParticipants(owner, removeBody(player)).andExpect(status().isOk()));

            assertThat(participantIdsOf(answer))
                    .doesNotContain(player.getId())
                    .contains(stayer.getId());
            assertThat(reloaded(playerCharacter).getCampaign()).isNull();
            assertThat(reloaded(stayerCharacter).getCampaign()).isNotNull();
        }

        @Test
        @DisplayName("the owner cannot remove themselves")
        void theOwnerCannotRemoveThemselves() throws Exception {
            patchParticipants(owner, removeBody(owner)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("a non-owner cannot change the roster")
        void nonOwnerCannotChangeTheRoster() throws Exception {
            patchParticipants(gm, removeBody(player)).andExpect(status().isForbidden());

            assertThat(reloaded(playerCharacter).getCampaign()).isNotNull();
        }
    }
}
