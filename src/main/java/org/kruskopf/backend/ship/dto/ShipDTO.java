package org.kruskopf.backend.ship.dto;

import org.kruskopf.backend.section.ContentSection;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read model for an Offworlders ship.
 *
 * @param sections  small, individually titled notes that sit beside {@code notes}
 *                  (Cargo, Passengers, Leads, Log). Plain text, in the order the
 *                  crew arranged them. {@code notes} stays a separate markdown
 *                  field and is never folded into this list.
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
        List<ContentSection> sections,
        String imageUrl,
        List<String> imageUrls,
        long version,
        LocalDateTime updatedAt
) {
}
