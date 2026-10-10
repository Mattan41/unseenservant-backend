package org.kruskopf.backend.ship;

import org.kruskopf.backend.ship.dto.ShipDTO;
import org.kruskopf.backend.ship.dto.ShipInputDTO;
import org.kruskopf.backend.ship.entity.Ship;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ShipMapper {

    public ShipDTO toDTO(Ship ship) {
        return new ShipDTO(
                ship.getId(),
                ship.getCampaign() != null ? ship.getCampaign().getId() : null,
                ship.getName(),
                ship.getHull(),
                ship.getHullMax(),
                ship.getArmor(),
                ship.getDamage(),
                ship.getDriveFuel(),
                ship.getMaxDriveFuel(),
                ship.getUpgrades() != null ? List.copyOf(ship.getUpgrades()) : List.of(),
                ship.getNotes(),
                ship.getVersion()
        );
    }

    /**
     * Copy the non-null fields of {@code dto} onto {@code ship}.
     */
    public void apply(ShipInputDTO dto, Ship ship) {
        if (dto.name() != null) {
            ship.setName(dto.name());
        }
        if (dto.hull() != null) {
            ship.setHull(dto.hull());
        }
        if (dto.hullMax() != null) {
            ship.setHullMax(dto.hullMax());
        }
        if (dto.armor() != null) {
            ship.setArmor(dto.armor());
        }
        if (dto.damage() != null) {
            ship.setDamage(dto.damage());
        }
        if (dto.driveFuel() != null) {
            ship.setDriveFuel(dto.driveFuel());
        }
        if (dto.maxDriveFuel() != null) {
            ship.setMaxDriveFuel(dto.maxDriveFuel());
        }
        if (dto.upgrades() != null) {
            ship.setUpgrades(new ArrayList<>(dto.upgrades()));
        }
        if (dto.notes() != null) {
            ship.setNotes(dto.notes());
        }
    }
}
