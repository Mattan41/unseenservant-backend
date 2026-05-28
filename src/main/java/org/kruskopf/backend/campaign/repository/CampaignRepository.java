package org.kruskopf.backend.campaign.repository;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findAllByParticipantsUserId(Long userId);
}