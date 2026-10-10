package org.kruskopf.backend.ship.dto;

import java.util.List;

/**
 * Write model for an Offworlders ship.
 * <p>
 * All fields are nullable and only applied when present, so a partial payload
 * never silently zeroes a value. {@code version} is the optimistic-concurrency
 * token returned by the previous read; a stale value is rejected with 409.
 */
public record ShipInputDTO(
        String name,
        Integer hull,
        Integer hullMax,
        Integer armor,
        String damage,
        Integer driveFuel,
        Integer maxDriveFuel,
        List<String> upgrades,
        String notes,
        Long version
) {
}
