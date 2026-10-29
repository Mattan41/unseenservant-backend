package org.kruskopf.backend.campaign.service;

import org.kruskopf.backend.addon.service.CampaignAddonService;
import org.kruskopf.backend.campaign.CampaignMapper;
import org.kruskopf.backend.campaign.CampaignParticipantFactory;
import org.kruskopf.backend.campaign.dto.CampaignResponseDTO;
import org.kruskopf.backend.campaign.dto.UpdateParticipantsDTO;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.playercharacter.service.GameCharacterService;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * The campaign's participant roster: who is in the campaign, under which
 * nickname and role, and who owns it.
 * <p>
 * This is the administration axis — add and remove participants, rename, change
 * a role, transfer ownership — and it is owner-only throughout, except that a
 * participant may rename themselves. The campaign's own fields and the
 * world-building side are {@link CampaignService}'s.
 * <p>
 * Two known follow-ups live here. The character unlink on removal keeps this
 * service coupled to the character domain: publishing a
 * {@code ParticipantRemovedEvent} instead is its own change, with its own
 * transaction semantics. And {@link #updateParticipants} still does both the add
 * and the remove half; it wants to be {@code addParticipants} /
 * {@code removeParticipants}.
 */
@Service
public class CampaignParticipantService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignParticipantService.class);
    private static final String USER_NOT_FOUND = "User not found with id: ";

    private final CampaignRepository campaignRepository;
    private final UserService userService;
    private final CampaignPermissionService campaignPermissionService;
    private final GameCharacterService gameCharacterService;
    private final CampaignAddonService campaignAddonService;
    private final CampaignMapper campaignMapper;
    private final CampaignParticipantFactory campaignParticipantFactory;

    public CampaignParticipantService(CampaignRepository campaignRepository,
                                      UserService userService,
                                      CampaignPermissionService campaignPermissionService,
                                      GameCharacterService gameCharacterService,
                                      CampaignAddonService campaignAddonService,
                                      CampaignMapper campaignMapper,
                                      CampaignParticipantFactory campaignParticipantFactory) {
        this.campaignRepository = campaignRepository;
        this.userService = userService;
        this.campaignPermissionService = campaignPermissionService;
        this.gameCharacterService = gameCharacterService;
        this.campaignAddonService = campaignAddonService;
        this.campaignMapper = campaignMapper;
        this.campaignParticipantFactory = campaignParticipantFactory;
    }

    /**
     * Updates the participants of a campaign by adding new users and removing existing ones.
     * Also triggers cleanup of character-campaign associations for removed users.
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
                    campaign.getParticipants().add(campaignParticipantFactory.create(campaign, user, participantDTO));
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

    // Helper methods

    /**
     * Maps the campaign these endpoints return. No viewer is passed in, so the
     * response carries no private GM notes — administration is not a
     * world-building read.
     */
    private CampaignResponseDTO toDTO(Campaign campaign) {
        return campaignMapper.toDTO(campaign, -1L, campaignAddonService.enabledAddonKeys(campaign.getId()));
    }

    private Campaign findCampaignOrThrow(long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
    }
}
