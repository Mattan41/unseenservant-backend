package org.kruskopf.backend.ship.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read model for an Offworlders ship.
 *
 * @param version   optimistic-concurrency token; echo it back on the next save
 * @param updatedAt when the ship row was last written. The client compares it
 *                  against its own "last opened" marker to decide whether to
 *                  flag the Ship section, so it moves on any save — a stat
 *                  change, a rename, an image added. Never use it as a
 *                  concurrency token: that is what {@code version} is for.
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
        int credits,
        List<String> upgrades,
        String notes,
        String imageUrl,
        List<String> imageUrls,
        long version,
        LocalDateTime updatedAt
) {
}
