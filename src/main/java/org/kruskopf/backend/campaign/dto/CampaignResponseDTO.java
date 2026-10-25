package org.kruskopf.backend.campaign.dto;

import org.kruskopf.backend.playercharacter.entity.GameSystem;

import java.util.List;

/**
 * One campaign.
 * <p>
 * {@code addons} lists the optional capabilities the campaign owner has switched
 * on, by their API keys (`planner`) — the client's navigation filters its
 * sections on it. Always present, empty when the campaign has none.
 */
public record CampaignResponseDTO(Long id, String name, String description, String worldLore, String privateDescription,
                                  String imageUrl, GameSystem primarySystem, Long ownerId,
                                  List<ParticipantResponseDTO> participants,
                                  List<String> addons) {

}