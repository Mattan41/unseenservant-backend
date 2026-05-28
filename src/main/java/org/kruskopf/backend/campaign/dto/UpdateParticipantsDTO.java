package org.kruskopf.backend.campaign.dto;

import java.util.List;

public record UpdateParticipantsDTO(List<ParticipantResponseDTO> participantsToAdd, List<Long> participantIdsToRemove) {
}

