package org.kruskopf.backend.campaign.dto;

import org.kruskopf.backend.playercharacter.entity.GameSystem;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One campaign.
 * <p>
 * {@code addons} lists the optional capabilities the campaign owner has switched
 * on, by their API keys (`planner`) — the client's navigation filters its
 * sections on it. Always present, empty when the campaign has none.
 * <p>
 * {@code createdAt} / {@code updatedAt} are the audit stamps the entity already
 * carries — Spring Data writes them through {@code BaseEntity}, so exposing them
 * needed no migration. {@code updatedAt} is what the client compares against its
 * own "last opened" marker to decide whether to flag the campaign's
 * <em>World Lore &amp; Background</em> section, which means it moves on <em>any</em>
 * campaign write — a rename, a new image, a participant or add-on change — not
 * only on a change to the lore text itself.
 */
public record CampaignResponseDTO(Long id, String name, String description, String worldLore, String privateDescription,
                                  String imageUrl, GameSystem primarySystem, Long ownerId,
                                  List<ParticipantResponseDTO> participants,
                                  List<String> addons,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {

}