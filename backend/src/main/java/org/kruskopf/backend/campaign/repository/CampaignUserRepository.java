package org.kruskopf.backend.campaign.repository;

import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignUserRepository extends JpaRepository<CampaignUser, Long> {
    List<CampaignUser> findByCampaignId(Long campaignId);

    List<CampaignUser> findByUserId(Long userId);

    boolean existsByCampaignIdAndUserId(Long campaignId, Long userId);
}