package org.kruskopf.backend.campaign.service;

import org.kruskopf.backend.campaign.dto.CampaignActivityDTO;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.message.repository.CampaignLastMessage;
import org.kruskopf.backend.message.repository.MessageRepository;
import org.kruskopf.backend.ship.entity.Ship;
import org.kruskopf.backend.ship.repository.ShipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The "what has changed" read model behind the client's campaign section badges:
 * one row per campaign the caller can see, each holding the change stamps of the
 * areas a badge can sit on (the campaign itself, its messages, its ship).
 * <p>
 * Read-only and deliberately cheap: the caller's campaign ids scope both side
 * lookups, so a whole list costs three queries — the campaigns, the newest
 * message per campaign, and the ships — instead of one per campaign. Nothing
 * here is per-user state; the client keeps its own "last opened" markers and
 * compares them against these timestamps (see `campaignActivity.js` there).
 * <p>
 * The scoping is exactly the one {@link CampaignService#getAllCampaignsForCurrentUser}
 * uses, so the set of campaigns that can carry a badge cannot drift from the set
 * the client lists. Unknown or other people's campaigns simply are not returned,
 * which is also the access control: there is nothing a caller could ask for that
 * is not their own campaign.
 */
@Service
public class CampaignActivityService {

    private final CampaignRepository campaignRepository;
    private final MessageRepository messageRepository;
    private final ShipRepository shipRepository;

    public CampaignActivityService(CampaignRepository campaignRepository, MessageRepository messageRepository, ShipRepository shipRepository) {
        this.campaignRepository = campaignRepository;
        this.messageRepository = messageRepository;
        this.shipRepository = shipRepository;
    }

    /**
     * @param userId the caller, from the authenticated principal
     * @return one {@link CampaignActivityDTO} per campaign the user participates
     *         in; empty when they are in none
     */
    @Transactional(readOnly = true)
    public List<CampaignActivityDTO> getActivityForCurrentUser(long userId) {
        List<Campaign> campaigns = campaignRepository.findAllByParticipantsUserId(userId);
        if (campaigns.isEmpty()) {
            return List.of();
        }

        List<Long> campaignIds = campaigns.stream().map(Campaign::getId).toList();

        Map<Long, LocalDateTime> lastMessageByCampaign = messageRepository
                .findLastMessageAtByCampaignIdIn(campaignIds)
                .stream()
                .collect(Collectors.toMap(CampaignLastMessage::campaignId, CampaignLastMessage::lastMessageAt));

        // A Map rather than a stream-toMap: a campaign without a ship must simply
        // be absent from the lookup, not a null value (Collectors.toMap refuses
        // null values outright).
        Map<Long, LocalDateTime> shipUpdatedByCampaign = new HashMap<>();
        for (Ship ship : shipRepository.findByCampaignIdIn(campaignIds)) {
            shipUpdatedByCampaign.put(ship.getCampaign().getId(), ship.getUpdatedAt());
        }

        return campaigns.stream()
                .map(campaign -> new CampaignActivityDTO(
                        campaign.getId(),
                        campaign.getUpdatedAt(),
                        lastMessageByCampaign.get(campaign.getId()),
                        shipUpdatedByCampaign.get(campaign.getId())))
                .toList();
    }
}
