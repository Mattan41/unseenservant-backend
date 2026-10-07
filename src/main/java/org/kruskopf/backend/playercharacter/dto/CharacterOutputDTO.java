package org.kruskopf.backend.playercharacter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataOutputDTO;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

/**
 * System-agnostic output payload for a character.
 * <p>
 * The nested {@code dnd5e} block is present only when
 * {@code systemType} is {@code DND5E}; it is {@code null} for other systems.
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
        LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt) {
}
