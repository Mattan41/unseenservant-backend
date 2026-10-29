package org.kruskopf.backend.message.repository;

import org.kruskopf.backend.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByCampaignId(Long campaignId);

    /**
     * When the newest message of each campaign was written, for the campaigns in
     * {@code campaignIds}. One query for a whole page of campaigns: the client's
     * section badges must not cost a query per campaign, and a campaign with no
     * messages simply has no row here.
     *
     * @param campaignIds the campaigns to look at; an empty collection answers empty
     * @return one row per campaign that has at least one message
     */
    @Query("SELECT m.campaign.id AS campaignId, MAX(m.createdAt) AS lastMessageAt FROM Message m " +
            "WHERE m.campaign.id IN :campaignIds GROUP BY m.campaign.id")
    List<CampaignLastMessage> findLastMessageAtByCampaignIdIn(@Param("campaignIds") Collection<Long> campaignIds);

}
