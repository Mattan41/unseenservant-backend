package org.kruskopf.backend.campaign.dto;

import java.util.List;

public record CampaignDTO(Long id, String name, String description, List<ParticipantDTO> participants) {

}
