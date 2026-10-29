package org.kruskopf.backend.ship.repository;

import org.kruskopf.backend.ship.entity.Ship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShipRepository extends JpaRepository<Ship, Long> {

    Optional<Ship> findByCampaignId(Long campaignId);

    boolean existsByCampaignId(Long campaignId);

    /**
     * The ships of the given campaigns, for the batched activity read model —
     * one query for a whole page of campaigns.
     *
     * @param campaignIds the campaigns to look at; an empty collection answers empty
     */
    List<Ship> findByCampaignIdIn(Collection<Long> campaignIds);
}
