package org.kruskopf.backend.playercharacter.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.kruskopf.backend.playercharacter.PlayerCharacterStats;

public record PlayerCharacterInputDTO(
        Long ownerId,
        Long campaignId,
        String name,
        @Min(0)
        @Max(20)
        Integer level,
        String characterClass,
        String imageUrl,
        String race,
        PlayerCharacterStats playerCharacterData) {
}
