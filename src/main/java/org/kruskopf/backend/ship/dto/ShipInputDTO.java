package org.kruskopf.backend.ship.dto;

import org.kruskopf.backend.section.ContentSection;

import java.util.List;

/**
 * Write model for an Offworlders ship.
 * <p>
 * All fields are nullable and only applied when present, so a partial payload
 * never silently zeroes a value. {@code version} is the optimistic-concurrency
 * token returned by the previous read; a stale value is rejected with 409.
 * <p>
 * {@code sections} is a whole-list replacement: the array sent is the array
 * stored, in that order. Omitting it leaves the stored sections untouched, which
 * is what lets the sheet's own save and a section edit travel separately.
 */
public record ShipInputDTO(
        String name,
        Integer hull,
        Integer hullMax,
        Integer armor,
        String damage,
        Integer driveFuel,
        Integer maxDriveFuel,
        Integer credits,
        List<String> upgrades,
        String notes,
        List<ContentSection> sections,
        String imageUrl,
        Long version
) {
}
