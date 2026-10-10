package org.kruskopf.backend.campaign.dto;

import org.kruskopf.backend.playercharacter.entity.GameSystem;

import java.util.List;

public record CampaignResponseDTO(Long id, String name, String description, String privateDescription, String imageUrl,
                                  GameSystem primarySystem, Long ownerId,
                                  List<ParticipantResponseDTO> participants) {

}