package org.kruskopf.backend.ship.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.ConcurrentUpdateException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.filestorage.StoredFileTransaction;
import org.kruskopf.backend.section.ContentSection;
import org.kruskopf.backend.ship.ShipMapper;
import org.kruskopf.backend.ship.dto.ShipDTO;
import org.kruskopf.backend.ship.dto.ShipInputDTO;
import org.kruskopf.backend.ship.entity.Ship;
import org.kruskopf.backend.ship.repository.ShipRepository;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShipService Unit Tests")
class ShipServiceTest {

    @Mock
    private ShipRepository shipRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignPermissionService campaignPermissionService;

    @Mock
    private StoredFileTransaction storedFileTransaction;

    @Spy
    private ShipMapper shipMapper = new ShipMapper();

    @InjectMocks
    private ShipService shipService;

    private static final long CAMPAIGN_ID = 10L;
    private static final long USER_ID = 1L;

    private Campaign campaign;

    @BeforeEach
    void setUp() {
        User owner = TestDataFactory.aUser("gm");
        ReflectionTestUtils.setField(owner, "id", USER_ID);
        campaign = TestDataFactory.aCampaign(owner);
        ReflectionTestUtils.setField(campaign, "id", CAMPAIGN_ID);
    }

    private Ship existingShip(int hull, long version) {
        Ship ship = Ship.createDefault();
        ship.setCampaign(campaign);
        ship.setHull(hull);
        ship.setVersion(version);
        ReflectionTestUtils.setField(ship, "id", 5L);
        return ship;
    }

    @Test
    @DisplayName("getShip returns the ship with the rulebook defaults")
    void getShip_returnsShip() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(existingShip(15, 0L)));

        ShipDTO dto = shipService.getShip(CAMPAIGN_ID, USER_ID);

        assertThat(dto.hull()).isEqualTo(Ship.DEFAULT_HULL);
        assertThat(dto.damage()).isEqualTo(Ship.DEFAULT_DAMAGE);
        assertThat(dto.maxDriveFuel()).isEqualTo(Ship.DEFAULT_MAX_DRIVE_FUEL);
        assertThat(dto.credits()).isEqualTo(Ship.DEFAULT_CREDITS);
    }

    @Test
    @DisplayName("getShip rejects non-participants")
    void getShip_rejectsNonParticipant() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, 99L)).thenReturn(false);

        assertThatThrownBy(() -> shipService.getShip(CAMPAIGN_ID, 99L))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    @DisplayName("getShip is a 404 when the campaign has no ship")
    void getShip_missingShip() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipService.getShip(CAMPAIGN_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("saveShip creates a default ship when none exists")
    void saveShip_createsDefaultShip() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.empty());
        when(shipRepository.saveAndFlush(any(Ship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipInputDTO input = new ShipInputDTO("The Desert Rose", null, null, null, null, null, null, null, null, null, null, null, null);
        ShipDTO dto = shipService.saveShip(CAMPAIGN_ID, input, USER_ID);

        assertThat(dto.name()).isEqualTo("The Desert Rose");
        assertThat(dto.hull()).isEqualTo(Ship.DEFAULT_HULL);
        assertThat(dto.version()).isZero();
    }

    @Test
    @DisplayName("saveShip rejects a stale version with a conflict")
    void saveShip_rejectsStaleVersion() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(existingShip(15, 3L)));

        ShipInputDTO stale = new ShipInputDTO(null, 10, null, null, null, null, null, null, null, null, null, null, 2L);

        assertThatThrownBy(() -> shipService.saveShip(CAMPAIGN_ID, stale, USER_ID))
                .isInstanceOf(ConcurrentUpdateException.class);
    }

    @Test
    @DisplayName("saveShip applies the update without touching the version (Hibernate owns it)")
    void saveShip_leavesVersionToHibernate() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        Ship ship = existingShip(15, 3L);
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(ship));
        when(shipRepository.saveAndFlush(any(Ship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipInputDTO input = new ShipInputDTO(null, 7, null, null, null, null, null, null, null, null, null, null, 3L);
        ShipDTO dto = shipService.saveShip(CAMPAIGN_ID, input, USER_ID);

        assertThat(dto.hull()).isEqualTo(7);
        // @Version makes Hibernate bump the version on flush. If the service also
        // incremented it the client's token would skip a number and every
        // subsequent save would look stale, so this must stay at 3.
        assertThat(ship.getVersion()).isEqualTo(3L);
    }

    @Test
    @DisplayName("saveShip applies a credits update")
    void saveShip_appliesCredits() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(existingShip(15, 3L)));
        when(shipRepository.saveAndFlush(any(Ship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipInputDTO input = new ShipInputDTO(null, null, null, null, null, null, null, 12, null, null, null, null, 3L);
        ShipDTO dto = shipService.saveShip(CAMPAIGN_ID, input, USER_ID);

        assertThat(dto.credits()).isEqualTo(12);
    }

    @Test
    @DisplayName("saveShip stores the sections in the order they were sent")
    void saveShip_storesSectionsInOrder() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        Ship ship = existingShip(15, 3L);
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(ship));
        when(shipRepository.saveAndFlush(any(Ship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ContentSection> sections = List.of(
                new ContentSection("s1", "Cargo", "Two crates of medical supplies."),
                new ContentSection("s2", "Passengers", "The Voss family, three of them."),
                new ContentSection("s3", "Leads", "A rumour about the Dry Dock."));

        ShipInputDTO input = new ShipInputDTO(
                null, null, null, null, null, null, null, null, null, null, sections, null, 3L);
        ShipDTO dto = shipService.saveShip(CAMPAIGN_ID, input, USER_ID);

        // The array is the crew's arrangement: nothing reorders it on the way in
        // or out, and there is no ordering column to disagree with it.
        assertThat(dto.sections()).containsExactlyElementsOf(sections);
        assertThat(dto.sections().get(0).title()).isEqualTo("Cargo");
    }

    @Test
    @DisplayName("saveShip leaves the stored sections alone when the payload omits them")
    void saveShip_keepsSectionsWhenOmitted() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        Ship ship = existingShip(15, 3L);
        ship.setSections(new ArrayList<>(List.of(new ContentSection("s1", "Cargo", "Still here."))));
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(ship));
        when(shipRepository.saveAndFlush(any(Ship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // The sheet's own save carries no `sections`; a section edit carries only
        // `sections`. Neither may wipe the other.
        ShipInputDTO input = new ShipInputDTO(null, 7, null, null, null, null, null, null, null, null, null, null, 3L);
        ShipDTO dto = shipService.saveShip(CAMPAIGN_ID, input, USER_ID);

        assertThat(dto.hull()).isEqualTo(7);
        assertThat(dto.sections()).hasSize(1);
        assertThat(dto.sections().get(0).body()).isEqualTo("Still here.");
    }

    @Test
    @DisplayName("getShip answers an empty section list for a ship that has none")
    void getShip_defaultsToNoSections() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(existingShip(15, 0L)));

        assertThat(shipService.getShip(CAMPAIGN_ID, USER_ID).sections()).isEmpty();
    }

    @Test
    @DisplayName("ensureDefaultShip returns the existing ship without creating another")
    void ensureDefaultShip_isIdempotent() {
        Ship existing = existingShip(15, 0L);
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(existing));

        Ship result = shipService.ensureDefaultShip(campaign);

        assertThat(result).isSameAs(existing);
    }
}
