package org.kruskopf.backend.playercharacter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.kruskopf.backend.playercharacter.PlayerCharacterStats;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

public record PlayerCharacterOutputDTO(
        Long id,
        @JsonProperty("ownerId") Long ownerId,
        @Nullable Long campaignId,
        String name,
        int level,
        String characterClass,
        String race,
        @JsonProperty("playerCharacterData") PlayerCharacterStats characterData,
        LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
}
