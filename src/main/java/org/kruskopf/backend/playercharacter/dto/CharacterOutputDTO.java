package org.kruskopf.backend.playercharacter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataOutputDTO;
import org.kruskopf.backend.offworlders.dto.OffworldersCharacterDataOutputDTO;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * System-agnostic output payload for a character.
 * <p>
 * The nested {@code dnd5e} / {@code offworlders} blocks are present only when
 * {@code systemType} matches that system; they are {@code null} otherwise, and
 * they are both {@code null} when {@code systemType} is {@code null} (a character
 * created without a game system).
 * <p>
 * {@code imageUrls} is the character's gallery (see
 * {@code POST /api/characters/{id}/images}); in an Offworlders campaign those
 * images, together with {@code notes}, are what the ship's crew manifest shows
 * on the character's card.
 */
public record CharacterOutputDTO(
        Long id,
        @JsonProperty("ownerId") Long ownerId,
        @Nullable Long campaignId,
        String name,
        @Nullable GameSystem systemType,
        @Nullable String notes,
        String avatarUrl,
        @Nullable List<String> imageUrls,
        @Nullable String appearance,
        @Nullable String backstory,
        @Nullable @JsonProperty("privateBackstory") String privateBackstory,
        @Nullable @JsonProperty("dnd5e") Dnd5eCharacterDataOutputDTO dnd5e,
        @Nullable @JsonProperty("offworlders") OffworldersCharacterDataOutputDTO offworlders,
        LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt) {
}
