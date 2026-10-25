package org.kruskopf.backend.addon.repository;

import org.kruskopf.backend.addon.AddonKey;
import org.kruskopf.backend.addon.entity.CampaignAddon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CampaignAddonRepository extends JpaRepository<CampaignAddon, Long> {

    Optional<CampaignAddon> findByCampaignIdAndAddonKey(Long campaignId, AddonKey addonKey);

    /**
     * Every enabled add-on of every listed campaign, in one query — the campaign
     * list endpoints map a page of campaigns and must not run one query per row.
     */
    List<CampaignAddon> findByCampaignIdIn(Collection<Long> campaignIds);
}
