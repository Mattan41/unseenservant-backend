package org.kruskopf.backend.playercharacter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataInputDTO;
import org.kruskopf.backend.offworlders.dto.OffworldersCharacterDataInputDTO;
import org.kruskopf.backend.playercharacter.entity.GameSystem;

/**
 * System-agnostic input payload for creating/updating a character.
 * <p>
 * System-specific data is carried in the optional {@code dnd5e} / {@code offworlders}
 * blocks and is only applied when {@code systemType} matches that system.
 */
public record CharacterInputDTO(
        Long ownerId,
        Long campaignId,
        String name,
        GameSystem systemType,
        String notes,
        String avatarUrl,
        String appearance,
        String backstory,
        @JsonProperty("privateBackstory") String privateBackstory,
        @JsonProperty("dnd5e") Dnd5eCharacterDataInputDTO dnd5e,
        @JsonProperty("offworlders") OffworldersCharacterDataInputDTO offworlders) {
}
