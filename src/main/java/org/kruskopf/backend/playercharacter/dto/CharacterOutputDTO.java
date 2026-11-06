package org.kruskopf.backend.playercharacter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataOutputDTO;
import org.kruskopf.backend.offworlders.dto.OffworldersCharacterDataOutputDTO;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.kruskopf.backend.section.ContentSection;
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
 * <p>
 * {@code sections} are small, individually titled plain-text notes that sit
 * beside {@code notes}. They belong to the character rather than to a campaign,
 * so they travel with it across campaigns, exports and imports. Everyone who may
 * see the character may read them; only the owner may write them (the PATCH is
 * owner-only).
 */
public record CharacterOutputDTO(
        Long id,
        @JsonProperty("ownerId") Long ownerId,
        @Nullable Long campaignId,
        String name,
        @Nullable GameSystem systemType,
        @Nullable String notes,
        @Nullable List<ContentSection> sections,
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
