package org.kruskopf.backend.offworlders.dto;

import org.kruskopf.backend.offworlders.OffworldersStats;

import java.util.List;

/**
 * Offworlders-specific data returned as the nested {@code offworlders} block of
 * a {@link org.kruskopf.backend.playercharacter.dto.CharacterOutputDTO}.
 */
public record OffworldersCharacterDataOutputDTO(
        String characterClass,
        String species,
        String look,
        int xp,
        int health,
        int armor,
        int supply,
        int supplyMax,
        OffworldersStats stats,
        List<String> skills,
        List<String> abilities) {
}
