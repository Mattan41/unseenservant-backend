package org.kruskopf.backend.ship.service;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.ConcurrentUpdateException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.ship.ShipMapper;
import org.kruskopf.backend.ship.dto.ShipDTO;
import org.kruskopf.backend.ship.dto.ShipInputDTO;
import org.kruskopf.backend.ship.entity.Ship;
import org.kruskopf.backend.ship.repository.ShipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Offworlders ship lifecycle, scoped to a campaign.
 * <p>
 * Every campaign participant may view and edit the ship (there is no delete).
 * Edits use optimistic concurrency: a client must send the version it loaded
 * and a stale write is rejected with {@link ConcurrentUpdateException} (409).
 */
@Service
public class ShipService {

    private final ShipRepository shipRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignPermissionService campaignPermissionService;
    private final ShipMapper shipMapper;

    public ShipService(ShipRepository shipRepository,
                       CampaignRepository campaignRepository,
                       CampaignPermissionService campaignPermissionService,
                       ShipMapper shipMapper) {
        this.shipRepository = shipRepository;
        this.campaignRepository = campaignRepository;
        this.campaignPermissionService = campaignPermissionService;
        this.shipMapper = shipMapper;
    }

    @Transactional(readOnly = true)
    public ShipDTO getShip(Long campaignId, long userId) {
        requireCampaign(campaignId);
        requireParticipant(campaignId, userId);

        return shipRepository.findByCampaignId(campaignId)
                .map(shipMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ship not found for campaign: " + campaignId));
    }

    /**
     * Upsert the ship: create it (with the rulebook defaults) if the campaign
     * has none yet, otherwise update it in place.
     */
    @Transactional
    public ShipDTO saveShip(Long campaignId, ShipInputDTO dto, long userId) {
        Campaign campaign = requireCampaign(campaignId);
        requireParticipant(campaignId, userId);

        Ship ship = shipRepository.findByCampaignId(campaignId).orElse(null);
        if (ship == null) {
            ship = Ship.createDefault();
            ship.setCampaign(campaign);
            shipMapper.apply(dto, ship);
        } else {
            if (dto.version() != null && dto.version() != ship.getVersion()) {
                throw new ConcurrentUpdateException(
                        "The ship was updated by someone else. Reload and try again.");
            }
            shipMapper.apply(dto, ship);
            ship.setVersion(ship.getVersion() + 1);
        }

        Ship savedShip = shipRepository.save(ship);
        return shipMapper.toDTO(savedShip);
    }

    /**
     * Create the default ship for a campaign if it does not already exist.
     * Idempotent: safe to call on every campaign update.
     */
    @Transactional
    public Ship ensureDefaultShip(Campaign campaign) {
        return shipRepository.findByCampaignId(campaign.getId())
                .orElseGet(() -> {
                    Ship ship = Ship.createDefault();
                    ship.setCampaign(campaign);
                    return shipRepository.save(ship);
                });
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign not found with id: " + campaignId));
    }

    private void requireParticipant(Long campaignId, long userId) {
        if (!campaignPermissionService.isParticipant(campaignId, userId)) {
            throw new UnauthorizedAccessException("User is not a participant in this campaign");
        }
    }
}
