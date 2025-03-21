package org.kruskopf.backend.campaign.dto;

import java.util.List;

public record CampaignCreationDTO(String name, String description, List<ParticipantResponseDTO> participants) {
}