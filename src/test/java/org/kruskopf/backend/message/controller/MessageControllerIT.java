package org.kruskopf.backend.message.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.kruskopf.backend.message.dto.MessageCreationDTO;
import org.kruskopf.backend.message.entity.Message;
import org.kruskopf.backend.message.repository.MessageRepository;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
@DisplayName("MessageController Integration Tests")
class MessageControllerIT extends AbstractIntegrationTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignUserRepository campaignUserRepository;
    private final MessageRepository messageRepository;
    private final JwtService jwtService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    MessageControllerIT(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            CampaignRepository campaignRepository,
            CampaignUserRepository campaignUserRepository,
            MessageRepository messageRepository,
            JwtService jwtService) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.campaignUserRepository = campaignUserRepository;
        this.messageRepository = messageRepository;
        this.jwtService = jwtService;
    }

    private User participant1;
    private User participant2;
    private User gm;
    private User outsider;
    private Campaign campaign;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        // Clean up in FK-safe order
        messageRepository.deleteAll();
        campaignUserRepository.deleteAll();
        campaignRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        participant1 = userRepository.save(TestDataFactory.aUser("participant1"));
        participant2 = userRepository.save(TestDataFactory.aUser("participant2"));
        gm = userRepository.save(TestDataFactory.aUser("gm"));
        outsider = userRepository.save(TestDataFactory.aUser("outsider"));

        // Create campaign
        campaign = TestDataFactory.aCampaign(gm);
        campaign = campaignRepository.save(campaign);

        // Add participants
        campaignUserRepository.save(new CampaignUser(campaign, participant1, CampaignRole.PLAYER, "Player 1"));
        campaignUserRepository.save(new CampaignUser(campaign, participant2, CampaignRole.PLAYER, "Player 2"));
        campaignUserRepository.save(new CampaignUser(campaign, gm, CampaignRole.GM, "Game Master"));

        // Create a test message from participant1
        testMessage = TestDataFactory.aMessage(campaign, participant1, "Test message from participant1");
        testMessage = messageRepository.save(testMessage);
    }

    private String getJwtToken(User user) {
        return jwtService.generateToken(user);
    }

    @Nested
    @DisplayName("GET /api/messages/campaign/{campaignId}")
    class GetMessagesByCampaignId {

        @Test
        @DisplayName("Returns 200 with messages for campaign participant")
        void returns200ForParticipant() throws Exception {
            String token = getJwtToken(participant1);

            mockMvc.perform(get("/api/messages/campaign/" + campaign.getId())
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].id").value(testMessage.getId()))
                    .andExpect(jsonPath("$[0].campaignId").value(campaign.getId()))
                    .andExpect(jsonPath("$[0].userId").value(participant1.getId()))
                    .andExpect(jsonPath("$[0].messageBody").value("Test message from participant1"))
                    .andExpect(jsonPath("$[0].createdAt").isNotEmpty());
        }

        @Test
        @DisplayName("Returns 403 for non-participant")
        void returns403ForNonParticipant() throws Exception {
            String token = getJwtToken(outsider);

            mockMvc.perform(get("/api/messages/campaign/" + campaign.getId())
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 404 for non-existent campaign (before participant check)")
        void returns404ForNonExistentCampaign() throws Exception {
            String token = getJwtToken(participant1);

            mockMvc.perform(get("/api/messages/campaign/99999")
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/messages/{id}")
    class GetMessageById {

        @Test
        @DisplayName("Returns 200 with message for campaign participant")
        void returns200ForParticipant() throws Exception {
            String token = getJwtToken(participant1);

            mockMvc.perform(get("/api/messages/" + testMessage.getId())
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testMessage.getId()))
                    .andExpect(jsonPath("$.campaignId").value(campaign.getId()))
                    .andExpect(jsonPath("$.userId").value(participant1.getId()))
                    .andExpect(jsonPath("$.messageBody").value("Test message from participant1"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        @DisplayName("Returns 403 for non-participant")
        void returns403ForNonParticipant() throws Exception {
            String token = getJwtToken(outsider);

            mockMvc.perform(get("/api/messages/" + testMessage.getId())
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 404 for non-existent message")
        void returns404ForNonExistentMessage() throws Exception {
            String token = getJwtToken(participant1);

            mockMvc.perform(get("/api/messages/99999")
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/messages")
    class CreateMessage {

        @Test
        @DisplayName("Returns 200 with created message for campaign participant")
        void returns200ForParticipant() throws Exception {
            String token = getJwtToken(participant2);
            MessageCreationDTO dto = new MessageCreationDTO(campaign.getId(), "New message from participant2");

            mockMvc.perform(post("/api/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.campaignId").value(campaign.getId()))
                    .andExpect(jsonPath("$.userId").value(participant2.getId()))
                    .andExpect(jsonPath("$.messageBody").value("New message from participant2"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        @DisplayName("Returns 403 for non-participant")
        void returns403ForNonParticipant() throws Exception {
            String token = getJwtToken(outsider);
            MessageCreationDTO dto = new MessageCreationDTO(campaign.getId(), "Message from outsider");

            mockMvc.perform(post("/api/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 400 for blank messageBody")
        void returns400ForBlankMessageBody() throws Exception {
            String token = getJwtToken(participant1);
            MessageCreationDTO dto = new MessageCreationDTO(campaign.getId(), "   ");

            mockMvc.perform(post("/api/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 400 for empty messageBody")
        void returns400ForEmptyMessageBody() throws Exception {
            String token = getJwtToken(participant1);
            MessageCreationDTO dto = new MessageCreationDTO(campaign.getId(), "");

            mockMvc.perform(post("/api/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 400 for messageBody exceeding 10000 characters")
        void returns400ForMessageBodyTooLong() throws Exception {
            String token = getJwtToken(participant1);
            String longMessage = "a".repeat(10001);
            MessageCreationDTO dto = new MessageCreationDTO(campaign.getId(), longMessage);

            mockMvc.perform(post("/api/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 404 for non-existent campaign")
        void returns404ForNonExistentCampaign() throws Exception {
            String token = getJwtToken(participant1);
            MessageCreationDTO dto = new MessageCreationDTO(99999L, "Message for non-existent campaign");

            mockMvc.perform(post("/api/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Ignores extraneous userId field in request JSON")
        void ignoresExtraneousUserIdField() throws Exception {
            String token = getJwtToken(participant1);
            String jsonWithUserId = String.format(
                    "{\"campaignId\": %d, \"messageBody\": \"Test\", \"userId\": 999}",
                    campaign.getId()
            );

            mockMvc.perform(post("/api/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithUserId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(participant1.getId())) // Should use authenticated user's ID
                    .andExpect(jsonPath("$.userId", not(equalTo(999)))); // Should not use the extraneous userId
        }

        @Test
        @DisplayName("CreatedAt is automatically populated via JPA auditing")
        void createdAtIsAutomaticallyPopulated() throws Exception {
            String token = getJwtToken(participant1);
            MessageCreationDTO dto = new MessageCreationDTO(campaign.getId(), "Test message for auditing");

            mockMvc.perform(post("/api/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());

            // Verify in database that createdAt was set
            Message savedMessage = messageRepository.findAll().getLast();
            assertThat(savedMessage.getCreatedAt()).isNotNull();
            // Note: updatedAt cannot be meaningfully exercised yet since there's no update/edit endpoint for messages.
            // This is a placeholder for when/if message editing is added.
        }
    }

    @Nested
    @DisplayName("DELETE /api/messages/{id}")
    class DeleteMessage {

        @Test
        @DisplayName("Returns 204 when message sender deletes their own message")
        void returns204WhenSenderDeletesOwnMessage() throws Exception {
            String token = getJwtToken(participant1);

            mockMvc.perform(delete("/api/messages/" + testMessage.getId())
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            // Verify message was deleted
            assertThat(messageRepository.findById(testMessage.getId())).isEmpty();
        }

        @Test
        @DisplayName("Returns 403 when different participant attempts to delete")
        void returns403WhenDifferentParticipantAttemptsToDelete() throws Exception {
            String token = getJwtToken(participant2);

            mockMvc.perform(delete("/api/messages/" + testMessage.getId())
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 403 when GM (not sender) attempts to delete another user's message")
        void returns403WhenGmAttemptsToDeleteOtherUsersMessage() throws Exception {
            // This is an intentional design decision - GM cannot delete other users' messages
            String token = getJwtToken(gm);

            mockMvc.perform(delete("/api/messages/" + testMessage.getId())
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 404 when deleting non-existent message")
        void returns404ForNonExistentMessage() throws Exception {
            String token = getJwtToken(participant1);

            mockMvc.perform(delete("/api/messages/99999")
                            .header("Authorization", "Bearer " + token))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }
}
