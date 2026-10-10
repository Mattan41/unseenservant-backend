package org.kruskopf.backend.offworlders.dto;

import org.kruskopf.backend.offworlders.OffworldersEntry;
import org.kruskopf.backend.offworlders.OffworldersItem;
import org.kruskopf.backend.offworlders.OffworldersStats;
import org.kruskopf.backend.offworlders.OffworldersWeapon;

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
        int credits,
        OffworldersStats stats,
        List<OffworldersEntry> skills,
        List<OffworldersEntry> abilities,
        List<OffworldersWeapon> weapons,
        List<OffworldersItem> items,
        int currentHealth,
        int healthModifier) {
}
