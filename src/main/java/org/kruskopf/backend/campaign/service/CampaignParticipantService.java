package org.kruskopf.backend.campaign.service;

import org.kruskopf.backend.addon.service.CampaignAddonService;
import org.kruskopf.backend.campaign.CampaignMapper;
import org.kruskopf.backend.campaign.CampaignParticipantFactory;
import org.kruskopf.backend.campaign.dto.CampaignResponseDTO;
import org.kruskopf.backend.campaign.dto.ParticipantResponseDTO;
import org.kruskopf.backend.campaign.dto.UpdateParticipantsDTO;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.event.ParticipantRemovedEvent;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
 * A removal is announced as a {@link ParticipantRemovedEvent} rather than acted on
 * here: unlinking the removed user's characters belongs to the character domain,
 * which listens for it, so nothing in this class knows characters exist.
 */
@Service
public class CampaignParticipantService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignParticipantService.class);
    private static final String USER_NOT_FOUND = "User not found with id: ";

    private final CampaignRepository campaignRepository;
    private final UserService userService;
    private final CampaignPermissionService campaignPermissionService;
    private final CampaignAddonService campaignAddonService;
    private final CampaignMapper campaignMapper;
    private final CampaignParticipantFactory campaignParticipantFactory;
    private final ApplicationEventPublisher applicationEventPublisher;

    public CampaignParticipantService(CampaignRepository campaignRepository,
                                      UserService userService,
                                      CampaignPermissionService campaignPermissionService,
                                      CampaignAddonService campaignAddonService,
                                      CampaignMapper campaignMapper,
                                      CampaignParticipantFactory campaignParticipantFactory,
                                      ApplicationEventPublisher applicationEventPublisher) {
        this.campaignRepository = campaignRepository;
        this.userService = userService;
        this.campaignPermissionService = campaignPermissionService;
        this.campaignAddonService = campaignAddonService;
        this.campaignMapper = campaignMapper;
        this.campaignParticipantFactory = campaignParticipantFactory;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Applies a roster change: the users the request names are added, and the ids it
     * names are removed. Owner-only, and the owner cannot be removed from their own
     * campaign.
     *
     * @param campaignId    the ID of the campaign to update
     * @param updateDTO     DTO containing the participants to add and the ids to remove
     * @param currentUserId the ID of the user performing the action (must be the owner)
     * @return the updated CampaignResponseDTO
     */
    @Transactional
    public CampaignResponseDTO updateParticipants(long campaignId, UpdateParticipantsDTO updateDTO, long currentUserId) {
        Campaign campaign = findCampaignOrThrow(campaignId);

        if (!campaign.isOwnedBy(currentUserId)) {
            throw new UnauthorizedAccessException("Only the campaign owner can update participants");
        }

        // A request that asks for nothing is answered with the current state.
        if (updateDTO.participantsToAdd() == null && updateDTO.participantIdsToRemove() == null) {
            return toDTO(campaign);
        }

        // Prevent owner from being removed
        if (updateDTO.participantIdsToRemove() != null && updateDTO.participantIdsToRemove().contains(campaign.getOwner().getId())) {
            throw new UnauthorizedAccessException("Owner cannot be removed from campaign");
        }

        addParticipants(campaign, updateDTO.participantsToAdd());
        removeParticipants(campaign, updateDTO.participantIdsToRemove());

        Campaign savedCampaign = campaignRepository.save(campaign);
        return toDTO(savedCampaign);
    }

    /**
     * Adds the users the request names, skipping anybody who is already on the roster:
     * a repeated add is a client convenience, unlike a removal of somebody who is not
     * there, which the removal half refuses outright.
     */
    private void addParticipants(Campaign campaign, List<ParticipantResponseDTO> participantsToAdd) {
        if (participantsToAdd == null || participantsToAdd.isEmpty()) {
            return;
        }

        participantsToAdd.forEach(participantDTO -> {
            User user = userService.findById(participantDTO.id())
                    .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + participantDTO.id()));

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

    /**
     * Removes the named participants, refusing the whole request when any of the ids is
     * not on the roster so that a typo cannot silently drop the rest. Each removal is
     * announced as a {@link ParticipantRemovedEvent} — the character domain unlinks that
     * user's characters off the back of it.
     */
    private void removeParticipants(Campaign campaign, List<Long> participantIdsToRemove) {
        if (participantIdsToRemove == null || participantIdsToRemove.isEmpty()) {
            return;
        }

        List<Long> nonExistingIds = participantIdsToRemove.stream()
                .filter(participantId -> campaign.getParticipants().stream()
                        .noneMatch(p -> p.getUser().getId().equals(participantId)))
                .toList();

        if (!nonExistingIds.isEmpty()) {
            throw new ResourceNotFoundException("The following users are not participants in the campaign: "
                    + String.join(", ", nonExistingIds.stream().map(String::valueOf).toList()));
        }

        participantIdsToRemove.forEach(userId ->
                applicationEventPublisher.publishEvent(new ParticipantRemovedEvent(campaign.getId(), userId)));

        campaign.getParticipants().removeIf(participant ->
                participantIdsToRemove.contains(participant.getUser().getId()));
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
