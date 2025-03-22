package org.kruskopf.backend.campaign.service;

import org.kruskopf.backend.campaign.dto.*;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.exception.ResourceNotFoundException;
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

    public CampaignService(CampaignRepository campaignRepository, UserService userService) {
        this.campaignRepository = campaignRepository;
        this.userService = userService;
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

    @Transactional
    public CampaignResponseDTO createCampaign(CampaignCreationDTO dto, Long ownerId) {
        Campaign campaign = new Campaign(dto.name(), dto.description());

        // Add owner as participant
        User owner = userService.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + ownerId));

        // define nickname, with fallback to user's display name

        campaign.getParticipants().add(new CampaignUser(
                campaign,
                owner,
                CampaignRole.GM, // Setting owner as GM per default
                "Game Master (" + owner.getDisplayName() + ")" // Assuming getDisplayName() returns the owner's display name
        ));

        // Add initial participants if provided
        if (dto.participants() != null && !dto.participants().isEmpty()) {
            dto.participants().forEach(participant -> {
                User user = userService.findById(participant.id())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + participant.id()));

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

    @Transactional
    public CampaignResponseDTO updateCampaign(Long id, CampaignUpdateDTO dto) {
        Campaign campaign = findCampaignOrThrow(id);

        campaign.setName(dto.name());
        campaign.setDescription(dto.description());

        Campaign savedCampaign = campaignRepository.save(campaign);
        return mapToResponseDTO(savedCampaign);
    }

    @Transactional
    public CampaignResponseDTO updateParticipants(Long id, UpdateParticipantsDTO updateDTO) {
        Campaign campaign = findCampaignOrThrow(id);

        // if no participants to add or remove, return the current state
        if (updateDTO.participantsToAdd() == null && updateDTO.participantIdsToRemove() == null) {
            return mapToResponseDTO(campaign);
        }


        if (!(updateDTO.participantsToAdd() == null || updateDTO.participantsToAdd().isEmpty())) {
            updateDTO.participantsToAdd().forEach(participantDTO -> {
                User user = userService.findById(participantDTO.id())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + participantDTO.id()));

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

    @Transactional
    public void deleteCampaign(Long id) {
        Campaign campaign = findCampaignOrThrow(id);
        campaignRepository.delete(campaign);
    }

    // Helper methods

    private Campaign findCampaignOrThrow(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
    }

    private CampaignResponseDTO mapToResponseDTO(Campaign campaign) {
        return new CampaignResponseDTO(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getParticipants().stream()
                        .map(participant -> new ParticipantResponseDTO(
                                participant.getUser().getId(),
                                participant.getNickname(),
                                participant.getRole().name()))
                        .toList()
        );
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
    public Campaign createCampaignRaw(Campaign campaign) {
        return campaignRepository.save(campaign);
    }

}
