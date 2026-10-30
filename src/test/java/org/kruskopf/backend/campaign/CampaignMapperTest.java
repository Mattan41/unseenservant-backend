package org.kruskopf.backend.campaign;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.campaign.dto.CampaignResponseDTO;
import org.kruskopf.backend.campaign.dto.ParticipantResponseDTO;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The mapping rules, above all the one that has to fail closed: the private
 * description (GM notes) is only ever included for a viewer who is a GM
 * <em>participant</em>, never for a non-GM owner and never for an unknown
 * viewer.
 */
@DisplayName("CampaignMapper Unit Tests")
class CampaignMapperTest {

    private final CampaignMapper mapper = new CampaignMapper();

    private static final long CAMPAIGN_ID = 10L;
    private static final long OWNER_ID = 1L;
    private static final long GM_ID = 2L;
    private static final long PLAYER_ID = 3L;
    private static final long OUTSIDER_ID = 99L;

    private User userWithId(long id) {
        User user = TestDataFactory.aUser("user" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    /**
     * A campaign whose owner is a plain PLAYER and whose GM is a different
     * participant, so the two axes can be told apart in the mapping.
     */
    private Campaign campaign() {
        Campaign campaign = TestDataFactory.aCampaign(userWithId(OWNER_ID));
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
        campaign.setDescription("Public description");
        campaign.setWorldLore("Public lore");
        campaign.setPrivateDescription("GM secrets");
        campaign.setImageUrl("/images/campaign_10.png");
        campaign.setPrimarySystem(GameSystem.DND5E);
        campaign.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
        campaign.setUpdatedAt(LocalDateTime.of(2024, 1, 2, 8, 30));
        campaign.getParticipants().add(new CampaignUser(campaign, userWithId(OWNER_ID), CampaignRole.PLAYER, "Owner"));
        campaign.getParticipants().add(new CampaignUser(campaign, userWithId(GM_ID), CampaignRole.GM, "Gm"));
        campaign.getParticipants().add(new CampaignUser(campaign, userWithId(PLAYER_ID), CampaignRole.PLAYER, "Player"));
        return campaign;
    }

    @Nested
    @DisplayName("toDTO(campaign, addons) — no viewer")
    class WithoutViewer {

        @Test
        @DisplayName("Maps the public fields and every participant")
        void mapsPublicFieldsAndParticipants() {
            CampaignResponseDTO dto = mapper.toDTO(campaign(), List.of());

            assertThat(dto.id()).isEqualTo(CAMPAIGN_ID);
            assertThat(dto.name()).isEqualTo("Test Campaign");
            assertThat(dto.description()).isEqualTo("Public description");
            assertThat(dto.worldLore()).isEqualTo("Public lore");
            assertThat(dto.imageUrl()).isEqualTo("/images/campaign_10.png");
            assertThat(dto.primarySystem()).isEqualTo(GameSystem.DND5E);
            assertThat(dto.ownerId()).isEqualTo(OWNER_ID);
            assertThat(dto.createdAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 12, 0));
            assertThat(dto.updatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 2, 8, 30));

            assertThat(dto.participants())
                    .extracting(ParticipantResponseDTO::id, ParticipantResponseDTO::nickname, ParticipantResponseDTO::role)
                    .containsExactly(
                            tuple(OWNER_ID, "Owner", "PLAYER"),
                            tuple(GM_ID, "Gm", "GM"),
                            tuple(PLAYER_ID, "Player", "PLAYER"));
        }

        @Test
        @DisplayName("Leaves the private description out, even though the campaign has a GM")
        void failsClosedWithoutViewer() {
            CampaignResponseDTO dto = mapper.toDTO(campaign(), List.of());

            assertThat(dto.privateDescription()).isNull();
            assertThat(dto.worldLore()).isEqualTo("Public lore");
        }

        @Test
        @DisplayName("Treats a null add-on list as no add-ons")
        void handlesNullAddons() {
            CampaignResponseDTO dto = mapper.toDTO(campaign(), null);

            assertThat(dto.addons()).isEmpty();
        }

        @Test
        @DisplayName("Passes the add-ons through unchanged")
        void passesAddonsThrough() {
            CampaignResponseDTO dto = mapper.toDTO(campaign(), List.of("planner"));

            assertThat(dto.addons()).containsExactly("planner");
        }

        @Test
        @DisplayName("Handles a campaign without an owner")
        void handlesNullOwner() {
            Campaign campaign = campaign();
            campaign.setOwner(null);

            CampaignResponseDTO dto = mapper.toDTO(campaign, List.of());

            assertThat(dto.ownerId()).isNull();
            assertThat(dto.privateDescription()).isNull();
        }

        @Test
        @DisplayName("Handles a campaign without participants")
        void handlesNoParticipants() {
            Campaign campaign = campaign();
            campaign.getParticipants().clear();

            CampaignResponseDTO dto = mapper.toDTO(campaign, List.of());

            assertThat(dto.participants()).isEmpty();
            assertThat(dto.privateDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("toDTO(campaign, viewerId, addons)")
    class WithViewer {

        @Test
        @DisplayName("A GM receives the private description")
        void gmSeesPrivateDescription() {
            CampaignResponseDTO dto = mapper.toDTO(campaign(), GM_ID, List.of());

            assertThat(dto.privateDescription()).isEqualTo("GM secrets");
        }

        @Test
        @DisplayName("A non-GM owner does not receive the private description but still gets the world lore")
        void nonGmOwnerDoesNotSeePrivateDescription() {
            CampaignResponseDTO dto = mapper.toDTO(campaign(), OWNER_ID, List.of());

            assertThat(dto.privateDescription()).isNull();
            assertThat(dto.worldLore()).isEqualTo("Public lore");
            assertThat(dto.participants()).hasSize(3);
        }

        @Test
        @DisplayName("A PLAYER participant does not receive the private description")
        void playerDoesNotSeePrivateDescription() {
            CampaignResponseDTO dto = mapper.toDTO(campaign(), PLAYER_ID, List.of());

            assertThat(dto.privateDescription()).isNull();
        }

        @Test
        @DisplayName("A viewer outside the campaign does not receive the private description")
        void outsiderDoesNotSeePrivateDescription() {
            CampaignResponseDTO dto = mapper.toDTO(campaign(), OUTSIDER_ID, List.of());

            assertThat(dto.privateDescription()).isNull();
        }
    }
}
