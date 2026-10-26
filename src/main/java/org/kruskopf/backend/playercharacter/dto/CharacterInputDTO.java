package org.kruskopf.backend.playercharacter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataInputDTO;
import org.kruskopf.backend.offworlders.dto.OffworldersCharacterDataInputDTO;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.springframework.lang.Nullable;

/**
 * System-agnostic input payload for creating/updating a character.
 * <p>
 * System-specific data is carried in the optional {@code dnd5e} / {@code offworlders}
 * blocks and is only applied when {@code systemType} matches that system.
 * {@code systemType} is optional too: a null on create stores a character with no
 * game system (only the generic core fields exist and no block is attached), and
 * on PATCH it means "leave the system unchanged".
 * <p>
 * The gallery ({@code imageUrls}) is deliberately absent: it is managed by the
 * dedicated image endpoints ({@code /api/characters/{id}/images}), never by this
 * form payload.
 */
public record CharacterInputDTO(
        Long ownerId,
        Long campaignId,
        String name,
        @Nullable GameSystem systemType,
        String notes,
        String avatarUrl,
        String appearance,
        String backstory,
        @JsonProperty("privateBackstory") String privateBackstory,
        @JsonProperty("dnd5e") Dnd5eCharacterDataInputDTO dnd5e,
        @JsonProperty("offworlders") OffworldersCharacterDataInputDTO offworlders) {
}
