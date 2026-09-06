package org.kruskopf.backend.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.message.dto.MessageCreationDTO;
import org.kruskopf.backend.message.dto.MessageDTO;
import org.kruskopf.backend.message.entity.Message;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageMapper Unit Tests")
class MessageMapperTest {

    private final MessageMapper mapper = new MessageMapper();

    @Nested
    @DisplayName("toDTO()")
    class ToDTO {

        @Test
        @DisplayName("Maps all fields correctly from fully populated Message entity")
        void mapsAllFieldsCorrectly() {
            // Arrange
            User user = TestDataFactory.aUser("testUser");
            ReflectionTestUtils.setField(user, "id", 1L);

            Campaign campaign = TestDataFactory.aCampaign(user);
            ReflectionTestUtils.setField(campaign, "id", 10L);

            Message message = TestDataFactory.aMessage(campaign, user, "Test message");
            ReflectionTestUtils.setField(message, "id", 100L);
            message.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
            message.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 12, 30));

            // Act
            MessageDTO dto = mapper.toDTO(message);

            // Assert
            assertThat(dto.id()).isEqualTo(100L);
            assertThat(dto.campaignId()).isEqualTo(10L);
            assertThat(dto.userId()).isEqualTo(1L);
            assertThat(dto.messageBody()).isEqualTo("Test message");
            assertThat(dto.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0));
            assertThat(dto.updatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 30));
        }

        @Test
        @DisplayName("Handles null campaign gracefully")
        void handlesNullCampaign() {
            // Arrange
            User user = TestDataFactory.aUser("testUser");
            ReflectionTestUtils.setField(user, "id", 1L);

            Message message = new Message();
            ReflectionTestUtils.setField(message, "id", 100L);
            message.setCampaign(null);
            message.setUser(user);
            message.setMessageBody("Test message");
            message.setCreatedAt(LocalDateTime.now());
            message.setUpdatedAt(null);

            // Act
            MessageDTO dto = mapper.toDTO(message);

            // Assert
            assertThat(dto.id()).isEqualTo(100L);
            assertThat(dto.campaignId()).isNull();
            assertThat(dto.userId()).isEqualTo(1L);
            assertThat(dto.messageBody()).isEqualTo("Test message");
            assertThat(dto.createdAt()).isNotNull();
            assertThat(dto.updatedAt()).isNull();
        }

        @Test
        @DisplayName("Handles null user gracefully")
        void handlesNullUser() {
            // Arrange
            User user = TestDataFactory.aUser("testUser");
            Campaign campaign = TestDataFactory.aCampaign(user);
            ReflectionTestUtils.setField(campaign, "id", 10L);

            Message message = new Message();
            ReflectionTestUtils.setField(message, "id", 100L);
            message.setCampaign(campaign);
            message.setUser(null);
            message.setMessageBody("Test message");
            message.setCreatedAt(LocalDateTime.now());
            message.setUpdatedAt(null);

            // Act
            MessageDTO dto = mapper.toDTO(message);

            // Assert
            assertThat(dto.id()).isEqualTo(100L);
            assertThat(dto.campaignId()).isEqualTo(10L);
            assertThat(dto.userId()).isNull();
            assertThat(dto.messageBody()).isEqualTo("Test message");
            assertThat(dto.createdAt()).isNotNull();
            assertThat(dto.updatedAt()).isNull();
        }

        @Test
        @DisplayName("Handles both null campaign and null user gracefully")
        void handlesNullCampaignAndUser() {
            // Arrange
            Message message = new Message();
            ReflectionTestUtils.setField(message, "id", 100L);
            message.setCampaign(null);
            message.setUser(null);
            message.setMessageBody("Test message");
            message.setCreatedAt(LocalDateTime.now());
            message.setUpdatedAt(null);

            // Act
            MessageDTO dto = mapper.toDTO(message);

            // Assert
            assertThat(dto.id()).isEqualTo(100L);
            assertThat(dto.campaignId()).isNull();
            assertThat(dto.userId()).isNull();
            assertThat(dto.messageBody()).isEqualTo("Test message");
            assertThat(dto.createdAt()).isNotNull();
            assertThat(dto.updatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {

        @Test
        @DisplayName("Creates Message with correct fields from DTO and User")
        void createsMessageWithCorrectFields() {
            // Arrange
            User user = TestDataFactory.aUser("testUser");
            ReflectionTestUtils.setField(user, "id", 1L);

            MessageCreationDTO dto = new MessageCreationDTO(10L, "Test message body");

            // Act
            Message message = mapper.toEntity(dto, user);

            // Assert
            assertThat(message.getMessageBody()).isEqualTo("Test message body");
            assertThat(message.getUser()).isEqualTo(user);
            assertThat(message.getUser().getId()).isEqualTo(1L);
            assertThat(message.getCampaign()).isNull(); // Campaign is set separately in service
        }

        @Test
        @DisplayName("Does not set campaign field (service handles this)")
        void doesNotSetCampaignField() {
            // Arrange
            User user = TestDataFactory.aUser("testUser");
            ReflectionTestUtils.setField(user, "id", 1L);

            MessageCreationDTO dto = new MessageCreationDTO(10L, "Test message");

            // Act
            Message message = mapper.toEntity(dto, user);

            // Assert
            assertThat(message.getCampaign()).isNull();
        }
    }
}
