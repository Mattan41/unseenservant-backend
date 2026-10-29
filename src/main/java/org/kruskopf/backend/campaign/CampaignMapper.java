package org.kruskopf.backend.campaign;

import org.kruskopf.backend.campaign.dto.CampaignResponseDTO;
import org.kruskopf.backend.campaign.dto.ParticipantResponseDTO;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps a campaign to the DTO the API returns.
 * <p>
 * The add-ons are passed in already loaded, which is what lets the list
 * endpoints fetch them for a whole page in one query instead of one per
 * campaign. A mapper that queried would hide that: the difference between the
 * single-campaign and the batched path is the caller's business.
 */
@Component
public class CampaignMapper {

    /** Maps without private fields (fails closed). */
    public CampaignResponseDTO toDTO(Campaign campaign, List<String> addons) {
        return toDTO(campaign, -1L, addons);
    }

    /**
     * Maps a campaign. The private description (GM notes) is only included when
     * the viewer is a campaign GM; everyone else — including a non-GM campaign
     * owner — receives null. World lore is public, so it is always included.
     * <p>
     * {@code addons} are the optional capabilities the campaign owner has switched
     * on, which is what the client's navigation filters on. They are public to
     * every participant (they describe the campaign's shape, not its secrets).
     */
    public CampaignResponseDTO toDTO(Campaign campaign, long viewerId, List<String> addons) {
        boolean includePrivate = campaign.getParticipants().stream().anyMatch(participant ->
                participant.getUser().getId().equals(viewerId)
                        && participant.getRole() == CampaignRole.GM);

        User owner = campaign.getOwner();

        return new CampaignResponseDTO(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getWorldLore(),
                includePrivate ? campaign.getPrivateDescription() : null,
                campaign.getImageUrl(),
                campaign.getPrimarySystem(),
                owner != null ? owner.getId() : null,
                campaign.getParticipants().stream()
                        .map(participant -> new ParticipantResponseDTO(
                                participant.getUser().getId(),
                                participant.getNickname(),
                                participant.getRole().name()))
                        .toList(),
                addons == null ? List.of() : addons,
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}
