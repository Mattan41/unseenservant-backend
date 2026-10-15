package org.kruskopf.backend.ship.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.filestorage.StoredFileTransaction;
import org.kruskopf.backend.ship.ShipCrewCharacter;
import org.kruskopf.backend.ship.dto.ShipCrewInputDTO;
import org.kruskopf.backend.ship.dto.ShipCrewMemberDTO;
import org.kruskopf.backend.ship.entity.Ship;
import org.kruskopf.backend.ship.entity.ShipCrewMember;
import org.kruskopf.backend.ship.repository.ShipCrewRepository;
import org.kruskopf.backend.ship.repository.ShipRepository;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShipCrewService Unit Tests")
class ShipCrewServiceTest {

    @Mock
    private ShipRepository shipRepository;
    @Mock
    private ShipCrewRepository shipCrewRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private CampaignUserRepository campaignUserRepository;
    @Mock
    private StoredFileTransaction storedFileTransaction;

    @InjectMocks
    private ShipCrewService shipCrewService;

    private static final long CAMPAIGN_ID = 10L;
    private static final long SHIP_ID = 5L;
    private static final long GM_ID = 1L;
    private static final long PLAYER_ID = 2L;

    private Campaign campaign;
    private User gm;
    private User player;

    @BeforeEach
    void setUp() {
        gm = TestDataFactory.aUser("gm");
        ReflectionTestUtils.setField(gm, "id", GM_ID);
        player = TestDataFactory.aUser("player");
        ReflectionTestUtils.setField(player, "id", PLAYER_ID);
        campaign = TestDataFactory.aCampaign(gm);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
    }

    private CampaignUser participant(User user, CampaignRole role, String nickname) {
        return new CampaignUser(campaign, user, role, nickname);
    }

    private Ship ship() {
        Ship ship = Ship.createDefault();
        ship.setCampaign(campaign);
        ReflectionTestUtils.setField(ship, "id", SHIP_ID);
        return ship;
    }

    private ShipCrewMember member(User user) {
        ShipCrewMember member = new ShipCrewMember(ship(), user);
        ReflectionTestUtils.setField(member, "id", 7L);
        return member;
    }

    @Test
    @DisplayName("getCrew returns one entry per current participant, in roster order")
    void getCrew_returnsOneEntryPerParticipant() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignUserRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(
                participant(gm, CampaignRole.GM, "Game Master (gm)"),
                participant(player, CampaignRole.PLAYER, "Vex's player")));
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(ship()));

        ShipCrewMember gmEntry = member(gm);
        gmEntry.setNotes("Captain on the bridge");
        when(shipCrewRepository.findByShipId(SHIP_ID)).thenReturn(List.of(gmEntry));

        List<ShipCrewMemberDTO> crew = shipCrewService.getCrew(CAMPAIGN_ID, GM_ID);

        assertThat(crew).hasSize(2);
        assertThat(crew.get(0).userId()).isEqualTo(GM_ID);
        assertThat(crew.get(0).nickname()).isEqualTo("Game Master (gm)");
        assertThat(crew.get(0).role()).isEqualTo("GM");
        assertThat(crew.get(0).notes()).isEqualTo("Captain on the bridge");
        // The player has no entry yet, so it comes back empty rather than missing.
        assertThat(crew.get(1).userId()).isEqualTo(PLAYER_ID);
        assertThat(crew.get(1).notes()).isEmpty();
        assertThat(crew.get(1).characters()).isEmpty();
    }

    @Test
    @DisplayName("getCrew rejects non-participants")
    void getCrew_rejectsNonParticipant() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignUserRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(
                participant(gm, CampaignRole.GM, "Game Master (gm)")));

        assertThatThrownBy(() -> shipCrewService.getCrew(CAMPAIGN_ID, 99L))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    @DisplayName("saveMyEntry creates the caller's entry with notes and characters")
    void saveMyEntry_createsEntry() {
        when(campaignUserRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(
                participant(player, CampaignRole.PLAYER, "Vex's player")));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(ship()));
        when(shipCrewRepository.findByShipIdAndUserId(SHIP_ID, PLAYER_ID)).thenReturn(Optional.empty());
        when(shipCrewRepository.save(any(ShipCrewMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShipCrewInputDTO input = new ShipCrewInputDTO(
                "Gunner, mostly.",
                List.of(new ShipCrewCharacter("Vex", "Pilot")));

        ShipCrewMemberDTO dto = shipCrewService.saveMyEntry(CAMPAIGN_ID, input, PLAYER_ID);

        assertThat(dto.userId()).isEqualTo(PLAYER_ID);
        assertThat(dto.notes()).isEqualTo("Gunner, mostly.");
        assertThat(dto.characters()).hasSize(1);
        assertThat(dto.characters().get(0).getName()).isEqualTo("Vex");
        assertThat(dto.characters().get(0).getRoleOnShip()).isEqualTo("Pilot");
    }

    @Test
    @DisplayName("addMyImage appends the stored URL to the caller's entry")
    void addMyImage_appendsUrl() {
        when(campaignUserRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(
                participant(gm, CampaignRole.GM, "Game Master (gm)")));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(ship()));
        when(shipCrewRepository.findByShipIdAndUserId(SHIP_ID, GM_ID))
                .thenReturn(Optional.of(member(gm)));
        MultipartFile file = mock(MultipartFile.class);
        // The real helper stores the file and then runs the callback inside a
        // transaction; stand in for that so the callback body is still exercised.
        when(storedFileTransaction.storeThenRun(eq(file), any(String.class), eq("IMAGE"),
                ArgumentMatchers.<Function<String, ShipCrewMemberDTO>>any()))
                .thenAnswer(invocation -> invocation
                        .<Function<String, ShipCrewMemberDTO>>getArgument(3)
                        .apply("stored.png"));
        when(shipCrewRepository.save(any(ShipCrewMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShipCrewMemberDTO dto = shipCrewService.addMyImage(CAMPAIGN_ID, file, GM_ID);

        assertThat(dto.imageUrls()).containsExactly("/images/stored.png");
    }

    @Test
    @DisplayName("removeMyImage drops the matching URL from the caller's entry")
    void removeMyImage_dropsUrl() {
        when(campaignUserRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(List.of(
                participant(gm, CampaignRole.GM, "Game Master (gm)")));
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(ship()));
        ShipCrewMember existing = member(gm);
        existing.setImageUrls(List.of("/images/a.png", "/images/b.png"));
        when(shipCrewRepository.findByShipIdAndUserId(SHIP_ID, GM_ID))
                .thenReturn(Optional.of(existing));
        when(shipCrewRepository.save(any(ShipCrewMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShipCrewMemberDTO dto = shipCrewService.removeMyImage(CAMPAIGN_ID, "/images/a.png", GM_ID);

        assertThat(dto.imageUrls()).containsExactly("/images/b.png");
    }
}
