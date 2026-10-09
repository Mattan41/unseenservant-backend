package org.kruskopf.backend.offworlders.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.kruskopf.backend.offworlders.OffworldersEntry;
import org.kruskopf.backend.offworlders.OffworldersItem;
import org.kruskopf.backend.offworlders.OffworldersStats;

import java.util.List;

/**
 * Offworlders-specific payload for creating/updating a character's system data.
 * All fields are null-safe so this can be used for partial PATCH requests.
 * <p>
 * Supply is capped at 3 by the rules (p.11); the numeric armor rating is capped
 * at 3 as on the character sheet.
 * <p>
 * {@code currentHealth} is the running HP (it may exceed {@code health} for
 * temporary HP); {@code healthModifier} is a manual ± adjustment to the derived
 * maximum Health (e.g. Hardy's +4 Health).
 */
public record OffworldersCharacterDataInputDTO(
        String characterClass,
        String species,
        String look,
        @Min(0) Integer xp,
        @Min(0) Integer health,
        @Min(0) @Max(3) Integer armor,
        @Min(0) @Max(3) Integer supply,
        @Min(0) @Max(3) Integer supplyMax,
        @Min(0) Integer credits,
        OffworldersStats stats,
        List<OffworldersEntry> skills,
        List<OffworldersEntry> abilities,
        List<OffworldersItem> items,
        @Min(0) Integer currentHealth,
        Integer healthModifier) {
}
