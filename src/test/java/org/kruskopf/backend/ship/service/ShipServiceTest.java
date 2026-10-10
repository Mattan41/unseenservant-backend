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
import org.kruskopf.backend.filestorage.FileStorageService;
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
    private FileStorageService fileStorageService;

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
        when(shipRepository.save(any(Ship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipInputDTO input = new ShipInputDTO("The Desert Rose", null, null, null, null, null, null, null, null, null, null);
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

        ShipInputDTO stale = new ShipInputDTO(null, 10, null, null, null, null, null, null, null, null, 2L);

        assertThatThrownBy(() -> shipService.saveShip(CAMPAIGN_ID, stale, USER_ID))
                .isInstanceOf(ConcurrentUpdateException.class);
    }

    @Test
    @DisplayName("saveShip applies the update and increments the version")
    void saveShip_incrementsVersion() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        when(shipRepository.findByCampaignId(CAMPAIGN_ID)).thenReturn(Optional.of(existingShip(15, 3L)));
        when(shipRepository.save(any(Ship.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipInputDTO input = new ShipInputDTO(null, 7, null, null, null, null, null, null, null, null, 3L);
        ShipDTO dto = shipService.saveShip(CAMPAIGN_ID, input, USER_ID);

        assertThat(dto.hull()).isEqualTo(7);
        assertThat(dto.version()).isEqualTo(4L);
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
