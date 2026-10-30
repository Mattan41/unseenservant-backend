package org.kruskopf.backend.campaign;

import org.kruskopf.backend.campaign.dto.ParticipantResponseDTO;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.user.dto.UserDTO;
import org.kruskopf.backend.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Builds the join row for a participant named by a DTO.
 * <p>
 * Both the role and the nickname fall back to a default when the request leaves
 * them out, and the fallback is what is worth having in one place: a campaign is
 * seeded with its participants at creation and later re-seeded through
 * {@code PATCH /api/campaigns/{id}/participants}.
 */
@Component
public class CampaignParticipantFactory {

    private static final Logger logger = LoggerFactory.getLogger(CampaignParticipantFactory.class);

    /** The join row for {@code user}, with the nickname and role the DTO asks for. */
    public CampaignUser create(Campaign campaign, User user, ParticipantResponseDTO participantDTO) {
        return new CampaignUser(campaign, user, resolveRole(participantDTO), resolveNickname(participantDTO, user));
    }

    /**
     * A blank role means PLAYER, and so does an unknown one — the latter with a
     * warning, because a value that is not a role can only come from a client
     * that does not follow this API.
     */
    private static CampaignRole resolveRole(ParticipantResponseDTO participantDTO) {
        try {
            return (participantDTO.role() == null || participantDTO.role().isBlank())
                    ? CampaignRole.PLAYER // Standard role
                    : CampaignRole.valueOf(participantDTO.role());
        } catch (IllegalArgumentException _) {
            logger.warn("Invalid role specified: {}. Using PLAYER as default.", participantDTO.role());
            return CampaignRole.PLAYER;
        }
    }

    /** The requested nickname, or the user's display name when none is asked for. */
    private static String resolveNickname(ParticipantResponseDTO participantDTO, User user) {
        if (participantDTO.nickname() != null && !participantDTO.nickname().isBlank()) {
            return participantDTO.nickname();
        }
        String displayName = UserDTO.fromUser(user).displayName();
        return displayName != null && !displayName.isBlank() ? displayName : user.getUserName();
    }
}
