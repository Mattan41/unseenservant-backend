package org.kruskopf.backend.campaign.service;

import org.kruskopf.backend.campaign.dto.*;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CampaignService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignService.class);
    public static final String USER_NOT_FOUND = "User not found with id: ";

    private final CampaignRepository campaignRepository;
    private final UserService userService;
    private final CampaignUserRepository campaignUserRepository;

    public CampaignService(CampaignRepository campaignRepository, UserService userService, CampaignUserRepository campaignUserRepository) {
        this.campaignRepository = campaignRepository;
        this.userService = userService;
        this.campaignUserRepository = campaignUserRepository;
    }

    @Transactional
    public CampaignResponseDTO createCampaign(CampaignCreationDTO dto) {
        Campaign campaign = new Campaign(dto.name(), dto.description());

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
        return mapToResponseDTO(savedCampaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponseDTO> getAllCampaigns() {
        return campaignRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CampaignResponseDTO> getAllCampaignsForCurrentUser(Long userId) {
        return campaignRepository.findAllByParticipantsUserId(userId).stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponseDTO getCampaignById(Long id) {
        Campaign campaign = findCampaignOrThrow(id);
        return mapToResponseDTO(campaign);
    }

    @Transactional(readOnly = true)
    public CampaignResponseDTO getCampaignByIdIfAuthorized(Long campaignId, Long userId) {

        Campaign campaign = findCampaignOrThrow(campaignId);

        // check if user is participant by searching thh campaign_user table
        boolean isParticipant = campaignUserRepository.existsByCampaignIdAndUserId(campaignId, userId);

        if (isParticipant)
            return mapToResponseDTO(campaign);

        // If user is not a participant, throw exception
        throw new UnauthorizedAccessException("User is not authorized to access this campaign");

    }


    @Transactional
    public CampaignResponseDTO updateCampaign(Long id, CampaignUpdateDTO dto, Long currentUserId) {
        Campaign campaign = findCampaignOrThrow(id);

        if (campaign.isOwnedBy(currentUserId)) {
            throw new UnauthorizedAccessException("Only the campaign owner can delete the campaign");
        }


        campaign.setName(dto.name());
        campaign.setDescription(dto.description());

        Campaign savedCampaign = campaignRepository.save(campaign);
        return mapToResponseDTO(savedCampaign);
    }

    @Transactional
    public CampaignResponseDTO updateParticipants(Long id, UpdateParticipantsDTO updateDTO, Long currentUserId) {
        Campaign campaign = findCampaignOrThrow(id);

        // Kontrollera ägarskap
        if (campaign.isOwnedBy(currentUserId))
            throw new UnauthorizedAccessException("Only the campaign owner can update participants");

        // if no participants to add or remove, return the current state
        if (updateDTO.participantsToAdd() == null && updateDTO.participantIdsToRemove() == null) {
            return mapToResponseDTO(campaign);
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
                        .anyMatch(p -> p.getUser().getId().equals(user.getId()));

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
            // todo: add check that we dont remove owner. also add owner of campaign
            List<Long> nonExistingIds = updateDTO.participantIdsToRemove().stream()
                    .filter(participantId -> campaign.getParticipants().stream()
                            .noneMatch(p -> p.getUser().getId().equals(participantId)))
                    .toList();

            if (!nonExistingIds.isEmpty()) {
                throw new ResourceNotFoundException("The following users are not participants in the campaign: "
                        + String.join(", ", nonExistingIds.stream().map(String::valueOf).toList()));
            }

            // remove the participants
            campaign.getParticipants().removeIf(participant ->
                    updateDTO.participantIdsToRemove().contains(participant.getUser().getId()));
        }


        Campaign savedCampaign = campaignRepository.save(campaign);
        return mapToResponseDTO(savedCampaign);
    }

    @Transactional
    public CampaignResponseDTO transferOwnership(Long campaignId, Long newOwnerId, Long currentUserId) {
        Campaign campaign = findCampaignOrThrow(campaignId);

        // Validate ownership
        if (campaign.isOwnedBy(currentUserId)) {
            throw new UnauthorizedAccessException("Only the campaign owner can transfer ownership");
        }

        // find new owner
        User newOwner = userService.findById(newOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + newOwnerId));

        // Validate that new owner is a participant
        boolean isParticipant = campaign.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(newOwnerId));

        if (!isParticipant) {
            throw new IllegalArgumentException("New owner must be a participant in the campaign");
        }

        // Transfer ownership
        campaign.setOwner(newOwner);
        Campaign savedCampaign = campaignRepository.save(campaign);

        return mapToResponseDTO(savedCampaign);
    }


    @Transactional
    public void deleteCampaign(Long id, Long currentUserId) {

        Campaign campaign = findCampaignOrThrow(id);

        if (campaign.isOwnedBy(currentUserId)) {
            throw new UnauthorizedAccessException("Only the campaign owner can update campaign details");
        }
        campaignRepository.delete(campaign);
    }


    // Helper methods

    private static CampaignRole getCampaignRole(ParticipantResponseDTO participantDTO) {
        CampaignRole effectiveRole;
        try {
            effectiveRole = (participantDTO.role() == null || participantDTO.role().isBlank())
                    ? CampaignRole.PLAYER // Standardroll
                    : CampaignRole.valueOf(participantDTO.role());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid role specified: {}. Using PLAYER as default.", participantDTO.role());
            effectiveRole = CampaignRole.PLAYER;
        }
        return effectiveRole;
    }

    private static String getEffectiveNickname(ParticipantResponseDTO participantDTO, User user) {
        return (participantDTO.nickname() == null || participantDTO.nickname().isBlank())
                ? UserDTO.fromUser(user).displayName()
                : participantDTO.nickname();
    }

    private Campaign findCampaignOrThrow(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
    }

    private CampaignResponseDTO mapToResponseDTO(Campaign campaign) {
        return new CampaignResponseDTO(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getOwner() != null ? campaign.getOwner().getId() : null,
                campaign.getParticipants().stream()
                        .map(participant -> new ParticipantResponseDTO(
                                participant.getUser().getId(),
                                participant.getNickname(),
                                participant.getRole().name()))
                        .toList()
        );
    }

    private String getEffectiveDisplayName(User user) {
        return user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : user.getFullName();
    }

    /**
     * For data initialization and testing only
     */
    @Transactional(readOnly = true)
    public List<Campaign> getAllCampaignsRaw() {
        return campaignRepository.findAll();
    }

    /**
     * For data initialization and testing only
     */
    @Transactional
    public void createCampaignRaw(Campaign campaign) {
        // Kontrollera att kampanjen har en ägare
        if (campaign.getOwner() == null) {
            if (campaign.getParticipants().isEmpty()) {
                throw new IllegalArgumentException("Campaign must have at least one participant to determine owner");
            }
            // Välj den första deltagaren som ägare
            User firstParticipant = campaign.getParticipants().getFirst().getUser();
            campaign.setOwner(firstParticipant);
        }
        campaignRepository.save(campaign);
    }

}
