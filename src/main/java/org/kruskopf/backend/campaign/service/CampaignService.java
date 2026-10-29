package org.kruskopf.backend.campaign.service;

import org.kruskopf.backend.addon.service.CampaignAddonService;
import org.kruskopf.backend.campaign.CampaignMapper;
import org.kruskopf.backend.campaign.dto.*;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.filestorage.StoredFileTransaction;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.kruskopf.backend.playercharacter.service.GameCharacterService;
import org.kruskopf.backend.ship.service.ShipService;
import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CampaignService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignService.class);
    public static final String USER_NOT_FOUND = "User not found with id: ";

    private final CampaignRepository campaignRepository;
    private final UserService userService;
    private final CampaignPermissionService campaignPermissionService;
    private final GameCharacterService gameCharacterService;
    private final StoredFileTransaction storedFileTransaction;
    private final ShipService shipService;
    private final CampaignAddonService campaignAddonService;
    private final CampaignMapper campaignMapper;

    public CampaignService(CampaignRepository campaignRepository, UserService userService, CampaignPermissionService campaignPermissionService, GameCharacterService gameCharacterService, StoredFileTransaction storedFileTransaction, ShipService shipService, CampaignAddonService campaignAddonService, CampaignMapper campaignMapper) {
        this.campaignRepository = campaignRepository;
        this.userService = userService;
        this.campaignPermissionService = campaignPermissionService;
        this.gameCharacterService = gameCharacterService;
        this.storedFileTransaction = storedFileTransaction;
        this.shipService = shipService;
        this.campaignAddonService = campaignAddonService;
        this.campaignMapper = campaignMapper;
    }

// TODO: Split the participant methods — updateParticipants (see the note on its
    //       growing complexity below), updateParticipantNickname, updateParticipantRole and
    //       transferOwnership, plus the getCampaignRole/getEffectiveNickname helpers — into a
    //       CampaignParticipantService. They map through CampaignMapper, which the campaign
    //       methods use as well, so the mapper already covers both. CampaignServiceTest, which
    //       injects this service's collaborators, has to move its participant tests with them.
    //       Two follow-ups on top of the split:
    //         • the character unlink on removal (gameCharacterService.removeAllCharactersFromCampaign)
    //           keeps the service coupled to the character domain — publishing
    //           ParticipantRemovedEvent/CampaignDeletedEvent via ApplicationEventPublisher is what
    //           would decouple it, and that is its own change with its own transaction semantics;
    //         • updateParticipants should itself be broken into addParticipants/removeParticipants.

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

                // Define nickname, with fallback to user's display name
                String effectiveNickname = getEffectiveNickname(participant, user);
                // Define role
                CampaignRole effectiveRole = getCampaignRole(participant);

                campaign.getParticipants().add(new CampaignUser(
                        campaign,
                        user,
                        effectiveRole,
                        effectiveNickname)
                );
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

    /**
     * Updates the participants of a campaign by adding new users and removing existing ones.
     * Also triggers cleanup of character-campaign associations for removed users.
     * TODO: This method is growing complex. Should be broken down into separate
     * extractable methods (e.g., addParticipants and removeParticipants).
     *
     * @param campaignId    the ID of the campaign to update
     * @param updateDTO     DTO containing lists of participant IDs to add or remove
     * @param currentUserId the ID of the user performing the action (must be the owner)
     * @return the updated CampaignResponseDTO
     */
    @Transactional
    public CampaignResponseDTO updateParticipants(long campaignId, UpdateParticipantsDTO updateDTO, long currentUserId) {
        Campaign campaign = findCampaignOrThrow(campaignId);

        // control owner
        if (!campaign.isOwnedBy(currentUserId))
            throw new UnauthorizedAccessException("Only the campaign owner can update participants");

        // if no participants to add or remove, return the current state
        if (updateDTO.participantsToAdd() == null && updateDTO.participantIdsToRemove() == null) {
            return toDTO(campaign);
        }

        // Prevent owner from being removed
        if (updateDTO.participantIdsToRemove() != null && updateDTO.participantIdsToRemove().contains(campaign.getOwner().getId())) {
            throw new UnauthorizedAccessException("Owner cannot be removed from campaign");
        }

        if (!(updateDTO.participantsToAdd() == null || updateDTO.participantsToAdd().isEmpty())) {
            updateDTO.participantsToAdd().forEach(participantDTO -> {
                User user = userService.findById(participantDTO.id())
                        .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + participantDTO.id()));

                // Check if user is already a participant
                boolean isExistingParticipant = campaign.getParticipants().stream()
                        .anyMatch(p -> Objects.equals(p.getUser().getId(), user.getId()));

                if (isExistingParticipant) {
                    // User is already a participant, log and skip
                    logger.info("User {} is already a participant in campaign {}. Skipping addition", user.getId(), campaign.getId());
                } else {
                    // Define nickname, with fallback to user's display name
                    String effectiveNickname = getEffectiveNickname(participantDTO, user);
                    // Define role
                    CampaignRole effectiveRole = getCampaignRole(participantDTO);

                    campaign.getParticipants().add(new CampaignUser(
                            campaign,
                            user,
                            effectiveRole,
                            effectiveNickname)
                    );
                }
            });
        }

        // Remove participants
        if (updateDTO.participantIdsToRemove() != null && !updateDTO.participantIdsToRemove().isEmpty()) {
            // Verify that all participant IDs to remove are actually participants in the campaign
            List<Long> nonExistingIds = updateDTO.participantIdsToRemove().stream()
                    .filter(participantId -> campaign.getParticipants().stream()
                            .noneMatch(p -> p.getUser().getId().equals(participantId)))
                    .toList();

            if (!nonExistingIds.isEmpty()) {
                throw new ResourceNotFoundException("The following users are not participants in the campaign: "
                        + String.join(", ", nonExistingIds.stream().map(String::valueOf).toList()));
            }

            // Remove campaign reference from all player characters of the participants to be removed
            for (long userId : updateDTO.participantIdsToRemove()) {
                gameCharacterService.removeAllCharactersFromCampaign(userId, campaignId);
            }

            // remove the participants
            campaign.getParticipants().removeIf(participant ->
                    updateDTO.participantIdsToRemove().contains(participant.getUser().getId()));
        }

        Campaign savedCampaign = campaignRepository.save(campaign);
        return toDTO(savedCampaign);
    }

    @Transactional
    public CampaignResponseDTO updateParticipantNickname(long campaignId, long participantId, String nickname, long currentUserId) {
        Campaign campaign = findCampaignOrThrow(campaignId);

        // A participant may rename themselves; otherwise only the campaign owner may.
        boolean isSelf = currentUserId == participantId;
        if (!isSelf && !campaignPermissionService.isCampaignOwner(campaignId, currentUserId)) {
            throw new UnauthorizedAccessException("Only the campaign owner can update another participant's nickname");
        }

        CampaignUser participant = campaign.getParticipants().stream()
                .filter(p -> p.getUser().getId() == participantId)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with id: " + participantId));

        participant.setNickname(nickname);
        campaignRepository.save(campaign);

        return toDTO(campaign);
    }


    @Transactional
    public CampaignResponseDTO updateParticipantRole(long campaignId, long participantId, String roleString, long currentUserId) {
        Campaign campaign = findCampaignOrThrow(campaignId);

        // Only the owner can change participant roles
        if (!campaign.isOwnedBy(currentUserId)) {
            throw new UnauthorizedAccessException("Only the campaign owner can change participant roles");
        }

        // convert string to enum
        CampaignRole role;
        try {
            role = CampaignRole.valueOf(roleString);
        } catch (IllegalArgumentException _) {
            // A client error, not a server fault: the caller sent a role that does not exist.
            throw new InvalidRequestException("Invalid role: " + roleString + ". Valid roles are PLAYER and GM.");
        }

        CampaignUser participant = campaign.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(participantId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with id: " + participantId));

        participant.setRole(role);
        campaignRepository.save(campaign);

        return toDTO(campaign);
    }

    // campaign management
    @Transactional
    public CampaignResponseDTO transferOwnership(long campaignId, long newOwnerId, long currentUserId) {
        Campaign campaign = findCampaignOrThrow(campaignId);

        // Validate ownership
        if (!campaign.isOwnedBy(currentUserId)) {
            throw new UnauthorizedAccessException("Only the campaign owner can transfer ownership");
        }

        // find new owner
        User newOwner = userService.findById(newOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + newOwnerId));

        if (newOwner.equals(campaign.getOwner())) {
            throw new InvalidRequestException("New owner must be different from the current owner");
        }

        // Validate that new owner is a participant
        boolean isParticipant = campaign.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(newOwnerId));

        if (!isParticipant) {
            throw new InvalidRequestException("New owner must be a participant in the campaign");
        }

        // Transfer ownership
        campaign.setOwner(newOwner);
        Campaign savedCampaign = campaignRepository.save(campaign);

        return toDTO(savedCampaign);
    }

    @Transactional
    public void deleteCampaign(long id, long currentUserId) {
        Campaign campaign = findCampaignOrThrow(id);

        if (!campaign.isOwnedBy(currentUserId)) {
            throw new UnauthorizedAccessException("Only the campaign owner can delete the campaign");
        }

        campaign.getParticipants().stream()
                .map(participant -> participant.getUser().getId())
                .forEach(userId -> gameCharacterService.removeAllCharactersFromCampaign(userId, id));


        campaignRepository.delete(campaign);
    }


    // Helper methods

    private static CampaignRole getCampaignRole(ParticipantResponseDTO participantDTO) {
        CampaignRole effectiveRole;
        try {
            effectiveRole = (participantDTO.role() == null || participantDTO.role().isBlank())
                    ? CampaignRole.PLAYER // Standard role
                    : CampaignRole.valueOf(participantDTO.role());
        } catch (IllegalArgumentException _) {
            logger.warn("Invalid role specified: {}. Using PLAYER as default.", participantDTO.role());
            effectiveRole = CampaignRole.PLAYER;
        }
        return effectiveRole;
    }

    private static String getEffectiveNickname(ParticipantResponseDTO participantDTO, User user) {
        if (participantDTO.nickname() != null && !participantDTO.nickname().isBlank()) {
            return participantDTO.nickname();
        }
        String displayName = UserDTO.fromUser(user).displayName();
        return displayName != null && !displayName.isBlank() ? displayName : user.getUserName();
    }

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
