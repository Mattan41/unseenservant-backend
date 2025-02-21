package org.kruskopf.backend.campaign.repository;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
}