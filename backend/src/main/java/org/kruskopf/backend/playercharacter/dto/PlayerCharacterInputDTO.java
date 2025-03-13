package org.kruskopf.backend.playercharacter.dto;

import org.kruskopf.backend.playercharacter.PlayerCharacterStats;

public record PlayerCharacterInputDTO(
//        @JsonProperty("ownerId") Long ownerId,
//        @Nullable Long campaignId,
//        String name,
//        int level,
//        String characterClass,
//        String race,
//        @JsonProperty("playerCharacterData") PlayerCharacterStats playerCharacterData

        Long ownerId,
        Long campaignId,
        String name,
        int level,
        String characterClass,
        String race,
        PlayerCharacterStats playerCharacterData) {
}
