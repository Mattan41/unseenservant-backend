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
import org.kruskopf.backend.message.entity.Message;
import org.kruskopf.backend.message.repository.MessageRepository;
import org.kruskopf.backend.ship.entity.Ship;
import org.kruskopf.backend.ship.repository.ShipRepository;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The campaign activity read model over HTTP: which campaigns can carry a badge,
 * the shape the client parses, and that {@code /activity} is never read as a
 * campaign id.
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("Campaign activity Integration Tests")
class CampaignActivityIT extends AbstractIntegrationTest {

    /** The wire format the client parses — see docs/INTEGRATION_CONTRACT.md. */
    private static final String ISO_LOCAL_DATE_TIME =
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignUserRepository campaignUserRepository;
    private final MessageRepository messageRepository;
    private final ShipRepository shipRepository;
    private final JwtService jwtService;
    private final EntityManager entityManager;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    CampaignActivityIT(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            CampaignRepository campaignRepository,
            CampaignUserRepository campaignUserRepository,
            MessageRepository messageRepository,
            ShipRepository shipRepository,
            JwtService jwtService,
            EntityManager entityManager) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.campaignUserRepository = campaignUserRepository;
        this.messageRepository = messageRepository;
        this.shipRepository = shipRepository;
        this.jwtService = jwtService;
        this.entityManager = entityManager;
    }

    private User member;
    private User outsider;
    private Campaign talkedIn;
    private Campaign quiet;
    private Campaign notMine;

    @BeforeEach
    void setUp() {
        // Clean up in FK-safe order
        shipRepository.deleteAll();
        messageRepository.deleteAll();
        campaignUserRepository.deleteAll();
        campaignRepository.deleteAll();
        userRepository.deleteAll();

        member = userRepository.save(TestDataFactory.aUser("member"));
        outsider = userRepository.save(TestDataFactory.aUser("outsider"));

        // The member's campaign with a ship and two messages…
        talkedIn = campaignRepository.save(TestDataFactory.aCampaign(member));
        campaignUserRepository.save(new CampaignUser(talkedIn, member, CampaignRole.PLAYER, "Member"));
        shipRepository.save(shipOf(talkedIn));
        messageRepository.save(TestDataFactory.aMessage(talkedIn, member, "First"));
        messageRepository.save(TestDataFactory.aMessage(talkedIn, member, "Second"));

        // …the member's campaign with neither a message nor a ship…
        quiet = campaignRepository.save(TestDataFactory.aCampaign(member));
        campaignUserRepository.save(new CampaignUser(quiet, member, CampaignRole.PLAYER, "Member"));

        // …and a campaign that is not the member's at all.
        notMine = campaignRepository.save(TestDataFactory.aCampaign(outsider));
        campaignUserRepository.save(new CampaignUser(notMine, outsider, CampaignRole.GM, "Outsider"));
    }

    private static Ship shipOf(Campaign campaign) {
        Ship ship = Ship.createDefault();
        ship.setCampaign(campaign);
        ship.setName("The Null Gravitas");
        return ship;
    }

    private String getJwtToken(User user) {
        return jwtService.generateToken(user);
    }

    /** The endpoint's answer for one caller, as JSON. */
    private JsonNode activityFor(User user) throws Exception {
        String body = mockMvc.perform(get("/api/campaigns/activity")
                        .header("Authorization", "Bearer " + getJwtToken(user)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }

    /** One campaign's row, or `null` when that campaign is not in the answer. */
    private JsonNode rowFor(JsonNode activity, Campaign campaign) {
        for (JsonNode row : activity) {
            if (row.path("campaignId").asLong() == campaign.getId()) {
                return row;
            }
        }
        return null;
    }

    /** The newest `createdAt` of a campaign's messages, as the database holds it. */
    private LocalDateTime newestMessageAt(Campaign campaign) {
        // Read it back through a cleared context on purpose: an entity still in
        // memory holds the nanoseconds the clock reported, while the column keeps
        // microseconds, and the endpoint answers the column's value.
        entityManager.flush();
        entityManager.clear();

        return messageRepository.findByCampaignId(campaign.getId()).stream()
                .map(Message::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    @Nested
    @DisplayName("GET /api/campaigns/activity")
    class GetCampaignActivity {

        @Test
        @DisplayName("answers one row per campaign the caller is in, with the three change stamps")
        void answersOneRowPerCampaign() throws Exception {
            JsonNode activity = activityFor(member);

            assertThat(activity.size()).isEqualTo(2);
            assertThat(rowFor(activity, talkedIn)).isNotNull();
            assertThat(rowFor(activity, quiet)).isNotNull();
            // The path is literal: "activity" is never resolved as a campaign id,
            // which would have answered a 400 (or a foreign campaign) instead.
            assertThat(rowFor(activity, talkedIn).get("campaignUpdatedAt").asString())
                    .matches(ISO_LOCAL_DATE_TIME);
        }

        @Test
        @DisplayName("reports the newest message of a campaign as its message stamp")
        void reportsTheNewestMessage() throws Exception {
            JsonNode row = rowFor(activityFor(member), talkedIn);

            LocalDateTime reported = LocalDateTime.parse(row.get("lastMessageAt").asString());

            assertThat(reported).isEqualTo(newestMessageAt(talkedIn));
            assertThat(row.get("shipUpdatedAt").asString()).matches(ISO_LOCAL_DATE_TIME);
        }

        @Test
        @DisplayName("leaves the message and ship stamps null where a campaign has neither")
        void leavesMissingStampsNull() throws Exception {
            JsonNode row = rowFor(activityFor(member), quiet);

            assertThat(row.has("lastMessageAt")).isTrue();
            assertThat(row.get("lastMessageAt").isNull()).isTrue();
            assertThat(row.has("shipUpdatedAt")).isTrue();
            assertThat(row.get("shipUpdatedAt").isNull()).isTrue();
        }

        @Test
        @DisplayName("never includes a campaign the caller is not in")
        void excludesForeignCampaigns() throws Exception {
            JsonNode activity = activityFor(member);

            assertThat(rowFor(activity, notMine)).isNull();
            // The outsider sees exactly their own campaign, not the member's two.
            assertThat(activityFor(outsider).size()).isEqualTo(1);
        }

        @Test
        @DisplayName("answers 401 without a token")
        void answers401WithoutAToken() throws Exception {
            mockMvc.perform(get("/api/campaigns/activity"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
