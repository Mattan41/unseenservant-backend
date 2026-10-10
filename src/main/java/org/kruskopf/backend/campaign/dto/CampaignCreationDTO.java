package org.kruskopf.backend.campaign.dto;

import org.kruskopf.backend.playercharacter.entity.GameSystem;

import java.util.List;

public record CampaignCreationDTO(String name, String description, Long ownerId,
                                  List<ParticipantResponseDTO> participants, GameSystem primarySystem) {
}