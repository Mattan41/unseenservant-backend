package org.kruskopf.backend.campaign.service;

import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.springframework.stereotype.Service;

@Service
public class CampaignPermissionService {

    private final CampaignRepository campaignRepository;
    private final CampaignUserRepository campaignUserRepository;

    public CampaignPermissionService(CampaignRepository campaignRepository,
                                     CampaignUserRepository campaignUserRepository) {
        this.campaignRepository = campaignRepository;
        this.campaignUserRepository = campaignUserRepository;
    }

    // --- Campaign-level ---

    public boolean isOwner(long campaignId, long userId) {
        return campaignRepository.findById(campaignId)
                .map(c -> c.isOwnedBy(userId))
                .orElse(false);
    }

    public boolean isParticipant(long campaignId, long userId) {
        return campaignUserRepository.existsByCampaignIdAndUserId(campaignId, userId);
    }

    public boolean hasRole(long campaignId, long userId, CampaignRole role) {
        return campaignUserRepository.existsByCampaignIdAndUserIdAndRole(campaignId, userId, role);
    }

    public boolean isGameMaster(long campaignId, long userId) {
        return hasRole(campaignId, userId, CampaignRole.GM);
    }

    // --- Character-level ---

    public boolean canViewCharacter(PlayerCharacter character, long userId) {
        boolean isOwner = character.getOwner().getId().equals(userId);
        if (isOwner) return true;
        if (character.getCampaign() != null) {
            return isGameMaster(character.getCampaign().getId(), userId);
        }
        return false;
    }

    public boolean canEditCharacter(PlayerCharacter character, long userId) {
        return character.getOwner().getId().equals(userId);
    }
}