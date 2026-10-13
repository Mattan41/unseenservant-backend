package org.kruskopf.backend.ship.dto;

import org.kruskopf.backend.ship.ShipCrewCharacter;

import java.util.List;

/**
 * Read model for one participant's crew entry.
 * <p>
 * The {@code nickname} and {@code role} are read from the participant's current
 * campaign membership (not stored on the entry), and one DTO is returned per
 * current participant — a participant without an entry yet gets empty fields.
 */
public record ShipCrewMemberDTO(
        Long userId,
        String nickname,
        String role,
        String notes,
        List<String> imageUrls,
        List<ShipCrewCharacter> characters) {
}
