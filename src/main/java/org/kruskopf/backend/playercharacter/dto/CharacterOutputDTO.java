package org.kruskopf.backend.playercharacter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataOutputDTO;
import org.kruskopf.backend.offworlders.dto.OffworldersCharacterDataOutputDTO;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

/**
 * System-agnostic output payload for a character.
 * <p>
 * The nested {@code dnd5e} / {@code offworlders} blocks are present only when
 * {@code systemType} matches that system; they are {@code null} otherwise.
 */
public record CharacterOutputDTO(
        Long id,
        @JsonProperty("ownerId") Long ownerId,
        @Nullable Long campaignId,
        String name,
        GameSystem systemType,
        @Nullable String notes,
        String avatarUrl,
        @Nullable @JsonProperty("dnd5e") Dnd5eCharacterDataOutputDTO dnd5e,
        @Nullable @JsonProperty("offworlders") OffworldersCharacterDataOutputDTO offworlders,
        LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt) {
}
