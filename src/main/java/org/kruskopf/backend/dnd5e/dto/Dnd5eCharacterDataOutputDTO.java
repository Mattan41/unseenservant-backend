package org.kruskopf.backend.dnd5e.dto;

import org.kruskopf.backend.dnd5e.Dnd5eCharacterStats;

/**
 * D&D 5e-specific data returned as the nested {@code dnd5e} block of a
 * {@link org.kruskopf.backend.playercharacter.dto.CharacterOutputDTO}.
 */
public record Dnd5eCharacterDataOutputDTO(
        int level,
        String characterClass,
        String race,
        int hitPoints,
        int armorClass,
        Dnd5eCharacterStats stats) {
}
