package org.kruskopf.backend.ship.repository;

import org.kruskopf.backend.ship.entity.Ship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipRepository extends JpaRepository<Ship, Long> {

    Optional<Ship> findByCampaignId(Long campaignId);

    boolean existsByCampaignId(Long campaignId);
}
