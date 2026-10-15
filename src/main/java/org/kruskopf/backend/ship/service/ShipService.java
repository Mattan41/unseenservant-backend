package org.kruskopf.backend.ship.service;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.ConcurrentUpdateException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.filestorage.StoredFileTransaction;
import org.kruskopf.backend.ship.ShipMapper;
import org.kruskopf.backend.ship.dto.ShipDTO;
import org.kruskopf.backend.ship.dto.ShipInputDTO;
import org.kruskopf.backend.ship.entity.Ship;
import org.kruskopf.backend.ship.repository.ShipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Offworlders ship lifecycle, scoped to a campaign.
 * <p>
 * Every campaign participant may view and edit the ship (there is no delete).
 * Edits use optimistic concurrency in two layers: the client must send the
 * version it loaded, and a mismatch against the freshly loaded ship is rejected
 * with {@link ConcurrentUpdateException} (409). That check cannot see a writer
 * that commits between the load and the flush, so {@code Ship.version} is also a
 * JPA {@code @Version} and Hibernate rejects that case too.
 */
@Service
public class ShipService {

    private final ShipRepository shipRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignPermissionService campaignPermissionService;
    private final StoredFileTransaction storedFileTransaction;
    private final ShipMapper shipMapper;

    public ShipService(ShipRepository shipRepository,
                       CampaignRepository campaignRepository,
                       CampaignPermissionService campaignPermissionService,
                       StoredFileTransaction storedFileTransaction,
                       ShipMapper shipMapper) {
        this.shipRepository = shipRepository;
        this.campaignRepository = campaignRepository;
        this.campaignPermissionService = campaignPermissionService;
        this.storedFileTransaction = storedFileTransaction;
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
        }

        // saveAndFlush, not save: @Version increments on flush, and the response
        // must carry the new version or the client's next save looks stale.
        Ship savedShip = shipRepository.saveAndFlush(ship);
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

    /**
     * Upload (or replace) the ship's profile image. Any participant may do this.
     * <p>
     * Deliberately not {@code @Transactional}: the file is written before the
     * transaction opens so no database connection is held during the write, and
     * {@link StoredFileTransaction} removes it again if the transaction fails.
     * Authorization has to happen first for the same reason — a refused request
     * must not touch the disk at all.
     */
    public ShipDTO uploadProfileImage(Long campaignId, MultipartFile file, long userId) {
        requireCampaign(campaignId);
        requireParticipant(campaignId, userId);

        return storedFileTransaction.storeThenRun(file, "ship_" + campaignId, "IMAGE", storedName -> {
            Ship ship = loadShipForEdit(campaignId, userId);
            ship.setImageUrl("/images/" + storedName);
            return shipMapper.toDTO(shipRepository.saveAndFlush(ship));
        });
    }

    /** Append an image to the ship's gallery (drawings, maps, etc.). */
    public ShipDTO addGalleryImage(Long campaignId, MultipartFile file, long userId) {
        requireCampaign(campaignId);
        requireParticipant(campaignId, userId);

        return storedFileTransaction.storeThenRun(file, "ship_" + campaignId + "_gallery", "IMAGE", storedName -> {
            Ship ship = loadShipForEdit(campaignId, userId);
            List<String> imageUrls = new ArrayList<>(ship.getImageUrls());
            imageUrls.add("/images/" + storedName);
            ship.setImageUrls(imageUrls);
            return shipMapper.toDTO(shipRepository.saveAndFlush(ship));
        });
    }

    /** Remove an image from the ship's gallery by its stored URL. */
    @Transactional
    public ShipDTO removeGalleryImage(Long campaignId, String url, long userId) {
        Ship ship = loadShipForEdit(campaignId, userId);
        List<String> imageUrls = new ArrayList<>(ship.getImageUrls());
        imageUrls.remove(url);
        ship.setImageUrls(imageUrls);
        return shipMapper.toDTO(shipRepository.saveAndFlush(ship));
    }

    /** Loads the ship for editing, creating the default one if none exists yet. */
    private Ship loadShipForEdit(Long campaignId, long userId) {
        Campaign campaign = requireCampaign(campaignId);
        requireParticipant(campaignId, userId);
        return shipRepository.findByCampaignId(campaignId).orElseGet(() -> {
            Ship ship = Ship.createDefault();
            ship.setCampaign(campaign);
            return ship;
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
