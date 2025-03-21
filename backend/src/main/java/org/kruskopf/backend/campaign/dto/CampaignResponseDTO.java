package org.kruskopf.backend.campaign.dto;

import java.util.List;

public record CampaignResponseDTO(Long id, String name, String description, List<ParticipantResponseDTO> participants) {

}