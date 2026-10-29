package org.kruskopf.backend.ship;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.ship.dto.ShipDTO;
import org.kruskopf.backend.ship.dto.ShipInputDTO;
import org.kruskopf.backend.ship.entity.Ship;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ShipMapper {

    public ShipDTO toDTO(Ship ship) {
        Campaign campaign = ship.getCampaign();
        List<String> upgrades = ship.getUpgrades();
        List<String> imageUrls = ship.getImageUrls();

        return new ShipDTO(
                ship.getId(),
                campaign != null ? campaign.getId() : null,
                ship.getName(),
                ship.getHull(),
                ship.getHullMax(),
                ship.getArmor(),
                ship.getDamage(),
                ship.getDriveFuel(),
                ship.getMaxDriveFuel(),
                ship.getCredits(),
                upgrades != null ? List.copyOf(upgrades) : List.of(),
                ship.getNotes(),
                ship.getImageUrl(),
                imageUrls != null ? List.copyOf(imageUrls) : List.of(),
                ship.getVersion(),
                ship.getUpdatedAt()
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
        if (dto.credits() != null) {
            ship.setCredits(dto.credits());
        }
        if (dto.upgrades() != null) {
            ship.setUpgrades(new ArrayList<>(dto.upgrades()));
        }
        if (dto.notes() != null) {
            ship.setNotes(dto.notes());
        }
        if (dto.imageUrl() != null) {
            ship.setImageUrl(dto.imageUrl());
        }
    }
}
