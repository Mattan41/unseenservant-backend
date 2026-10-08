package org.kruskopf.backend.dnd5e.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.kruskopf.backend.dnd5e.Dnd5eCharacterStats;

/**
 * D&D 5e-specific payload for creating/updating a character's system data.
 * All fields are null-safe so this can be used for partial PATCH requests.
 */
public record Dnd5eCharacterDataInputDTO(
        @Min(1)
        @Max(20)
        Integer level,
        String characterClass,
        String race,
        Integer hitPoints,
        Integer armorClass,
        Dnd5eCharacterStats stats) {
}
