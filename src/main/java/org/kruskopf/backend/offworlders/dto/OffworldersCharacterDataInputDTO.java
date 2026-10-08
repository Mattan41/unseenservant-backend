package org.kruskopf.backend.offworlders.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.kruskopf.backend.offworlders.OffworldersStats;

import java.util.List;

/**
 * Offworlders-specific payload for creating/updating a character's system data.
 * All fields are null-safe so this can be used for partial PATCH requests.
 */
public record OffworldersCharacterDataInputDTO(
        String characterClass,
        String species,
        String look,
        @Min(0) Integer xp,
        @Min(0) Integer health,
        @Min(0) @Max(3) Integer armor,
        @Min(0) Integer supply,
        @Min(0) Integer supplyMax,
        OffworldersStats stats,
        List<String> skills,
        List<String> abilities) {
}
