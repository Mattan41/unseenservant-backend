package org.kruskopf.backend.ship.dto;

import java.util.List;

/**
 * Read model for an Offworlders ship.
 *
 * @param version optimistic-concurrency token; echo it back on the next save
 */
public record ShipDTO(
        Long id,
        Long campaignId,
        String name,
        int hull,
        int hullMax,
        int armor,
        String damage,
        int driveFuel,
        int maxDriveFuel,
        List<String> upgrades,
        String notes,
        String imageUrl,
        List<String> imageUrls,
        long version
) {
}
