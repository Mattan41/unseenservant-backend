package org.kruskopf.backend.playercharacter.dto;

import jakarta.validation.constraints.NotNull;

/** Body of {@code PATCH /api/characters/{id}/campaign}. */
public record CharacterCampaignUpdateDTO(@NotNull Long campaignId) {
}
