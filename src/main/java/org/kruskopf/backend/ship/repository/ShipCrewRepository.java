package org.kruskopf.backend.ship.repository;

import org.kruskopf.backend.ship.entity.ShipCrewMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipCrewRepository extends JpaRepository<ShipCrewMember, Long> {

    List<ShipCrewMember> findByShipId(Long shipId);

    Optional<ShipCrewMember> findByShipIdAndUserId(Long shipId, Long userId);
}
