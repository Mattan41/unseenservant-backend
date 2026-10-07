package org.kruskopf.backend.playercharacter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataInputDTO;
import org.kruskopf.backend.playercharacter.entity.GameSystem;

/**
 * System-agnostic input payload for creating/updating a character.
 * <p>
 * System-specific data is carried in the optional {@code dnd5e} block and is
 * only applied when {@code systemType} is {@code DND5E}.
 */
public record CharacterInputDTO(
        Long ownerId,
        Long campaignId,
        String name,
        GameSystem systemType,
        String notes,
        String avatarUrl,
        @JsonProperty("dnd5e") Dnd5eCharacterDataInputDTO dnd5e) {
}
