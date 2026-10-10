package org.kruskopf.backend.campaign.dto;

import org.kruskopf.backend.playercharacter.entity.GameSystem;

public record CampaignUpdateDTO(String name, String description, String privateDescription, GameSystem primarySystem) {
}
