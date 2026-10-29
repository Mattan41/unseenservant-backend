package org.kruskopf.backend.campaign.service;

import org.kruskopf.backend.addon.service.CampaignAddonService;
import org.kruskopf.backend.campaign.CampaignMapper;
import org.kruskopf.backend.campaign.CampaignParticipantFactory;
import org.kruskopf.backend.campaign.dto.*;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.event.CampaignDeletedEvent;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.filestorage.StoredFileTransaction;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.kruskopf.backend.ship.service.ShipService;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
public class CampaignService {

    private static final String USER_NOT_FOUND = "User not found with id: ";

    private final CampaignRepository campaignRepository;
    private final UserService userService;
    private final CampaignPermissionService campaignPermissionService;
    private final StoredFileTransaction storedFileTransaction;
    private final ShipService shipService;
    private final CampaignAddonService campaignAddonService;
    private final CampaignMapper campaignMapper;
    private final CampaignParticipantFactory campaignParticipantFactory;
    private final ApplicationEventPublisher applicationEventPublisher;

    public CampaignService(CampaignRepository campaignRepository, UserService userService, CampaignPermissionService campaignPermissionService, StoredFileTransaction storedFileTransaction, ShipService shipService, CampaignAddonService campaignAddonService, CampaignMapper campaignMapper, CampaignParticipantFactory campaignParticipantFactory, ApplicationEventPublisher applicationEventPublisher) {
        this.campaignRepository = campaignRepository;
        this.userService = userService;
        this.campaignPermissionService = campaignPermissionService;
        this.storedFileTransaction = storedFileTransaction;
        this.shipService = shipService;
        this.campaignAddonService = campaignAddonService;
        this.campaignMapper = campaignMapper;
        this.campaignParticipantFactory = campaignParticipantFactory;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public CampaignResponseDTO createCampaign(CampaignCreationDTO dto) {
        Campaign campaign = new Campaign(dto.name(), dto.description());
        campaign.setWorldLore(dto.worldLore());
        campaign.setPrivateDescription(dto.privateDescription());
        campaign.setPrimarySystem(dto.primarySystem());

        // Add owner as participant
        User owner = userService.findById(dto.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + dto.ownerId()));

        // set owner of campaign
        campaign.setOwner(owner);

        // define nickname, with fallback to user's display name

        campaign.getParticipants().add(new CampaignUser(
                campaign,
                owner,
                CampaignRole.GM, // Setting owner as GM per default
                "Game Master (" + getEffectiveDisplayName(owner) + ")" //
        ));

        // Add initial participants if provided
        if (dto.participants() != null && !dto.participants().isEmpty()) {
            dto.participants().forEach(participant -> {
                User user = userService.findById(participant.id())
                        .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + participant.id()));

                campaign.getParticipants().add(campaignParticipantFactory.create(campaign, user, participant));
            });
        }

        Campaign savedCampaign = campaignRepository.save(campaign);
        createDefaultShipIfOffworlders(savedCampaign);
        return toDTO(savedCampaign, dto.ownerId());
    }

    @Transactional(readOnly = true)
    public List<CampaignResponseDTO> getAllCampaigns() {
        List<Campaign> campaigns = campaignRepository.findAll();
        Map<Long, List<String>> addonsByCampaign = campaignAddonService.enabledAddonKeysFor(idsOf(campaigns));
        return campaigns.stream()
                .map(campaign -> campaignMapper.toDTO(campaign, -1L,
                        addonsByCampaign.getOrDefault(campaign.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CampaignResponseDTO> getAllCampaignsForCurrentUser(long userId) {
        List<Campaign> campaigns = campaignRepository.findAllByParticipantsUserId(userId);
        Map<Long, List<String>> addonsByCampaign = campaignAddonService.enabledAddonKeysFor(idsOf(campaigns));
        return campaigns.stream()
                .map(campaign -> campaignMapper.toDTO(campaign, userId,
                        addonsByCampaign.getOrDefault(campaign.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponseDTO getCampaignById(long id) {
        Campaign campaign = findCampaignOrThrow(id);
        return toDTO(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignResponseDTO getCampaignByIdIfAuthorized(long campaignId, long userId) {

        Campaign campaign = findCampaignOrThrow(campaignId);

        boolean isParticipant = campaignPermissionService.isParticipant(campaignId, userId);

        if (isParticipant)
            return toDTO(campaign, userId);

        // If user is not a participant, throw exception
        throw new UnauthorizedAccessException("User is not authorized to access this campaign");

    }


    @Transactional
    public CampaignResponseDTO updateCampaign(long id, CampaignUpdateDTO dto, long currentUserId) {
        Campaign campaign = findCampaignOrThrow(id);

        if (!campaignPermissionService.isGameMaster(id, currentUserId)) {
            throw new UnauthorizedAccessException("Only a campaign GM can update the campaign");
        }


        campaign.setName(dto.name());
        campaign.setDescription(dto.description());
        campaign.setWorldLore(dto.worldLore());
        campaign.setPrivateDescription(dto.privateDescription());
        campaign.setPrimarySystem(dto.primarySystem());

        Campaign savedCampaign = campaignRepository.save(campaign);
        createDefaultShipIfOffworlders(savedCampaign);
        return toDTO(savedCampaign, currentUserId);
    }

    /**
     * Upload (or replace) the campaign image. GM only.
     * <p>
     * Deliberately not {@code @Transactional}: the file is written before the
     * transaction opens so no database connection is held during the write, and
     * {@link StoredFileTransaction} removes it again if the transaction fails.
     * The 404 and permission checks have to come first for the same reason — a
     * refused request must not touch the disk.
     */
    public CampaignResponseDTO uploadCampaignImage(long id, MultipartFile file, long currentUserId) {
        findCampaignOrThrow(id);

        if (!campaignPermissionService.isGameMaster(id, currentUserId)) {
            throw new UnauthorizedAccessException("Only a campaign GM can update the campaign image");
        }

        return storedFileTransaction.storeThenRun(file, "campaign_" + id, "IMAGE", storedName -> {
            Campaign campaign = findCampaignOrThrow(id);
            campaign.setImageUrl("/images/" + storedName);
            return toDTO(campaignRepository.save(campaign));
        });
    }

    @Transactional
    public void deleteCampaign(long id, long currentUserId) {
        Campaign campaign = findCampaignOrThrow(id);

        if (!campaign.isOwnedBy(currentUserId)) {
            throw new UnauthorizedAccessException("Only the campaign owner can delete the campaign");
        }

        // Announced before the campaign goes, because the listener clears the campaign
        // reference on every character that pointed at it, and those rows hold a foreign
        // key to this one. It runs in this transaction (see CampaignMembershipListener),
        // so a delete that rolls back takes the unlinking with it.
        applicationEventPublisher.publishEvent(new CampaignDeletedEvent(id, campaign.getParticipants().stream()
                .map(participant -> participant.getUser().getId())
                .toList()));

        campaignRepository.delete(campaign);
    }


    // Helper methods

    private Campaign findCampaignOrThrow(long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
    }

    /**
     * An Offworlders campaign always owns exactly one ship. Create the default
     * ship the first time the primary system becomes OFFWORLDERS (idempotent).
     */
    private void createDefaultShipIfOffworlders(Campaign campaign) {
        if (campaign.getPrimarySystem() == GameSystem.OFFWORLDERS) {
            shipService.ensureDefaultShip(campaign);
        }
    }

    /** Maps one campaign with no viewer identity: private fields are left out. */
    private CampaignResponseDTO toDTO(Campaign campaign) {
        return toDTO(campaign, -1L);
    }

    /**
     * Maps one campaign, looking its enabled add-ons up on the way. The list
     * endpoints call {@link CampaignMapper} directly instead, so a page of
     * campaigns costs one add-on query rather than one each.
     */
    private CampaignResponseDTO toDTO(Campaign campaign, long viewerId) {
        return campaignMapper.toDTO(campaign, viewerId, campaignAddonService.enabledAddonKeys(campaign.getId()));
    }

    /** The ids of a page of campaigns, for one batched add-on lookup. */
    private static List<Long> idsOf(List<Campaign> campaigns) {
        return campaigns.stream().map(Campaign::getId).toList();
    }

    private String getEffectiveDisplayName(User user) {
        String displayName = user.getDisplayName();
        return displayName != null && !displayName.isBlank()
                ? displayName
                : user.getFullName();
    }

}
