package org.kruskopf.backend.ship.dto;

import org.kruskopf.backend.ship.ShipCrewCharacter;

import java.util.List;

/**
 * Write model for the caller's own crew entry.
 * <p>
 * Both fields are optional: a {@code null} leaves the stored value unchanged,
 * so the entry can be edited field-by-field.
 */
public record ShipCrewInputDTO(
        String notes,
        List<ShipCrewCharacter> characters) {
}
