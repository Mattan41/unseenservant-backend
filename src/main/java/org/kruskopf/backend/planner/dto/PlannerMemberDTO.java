package org.kruskopf.backend.planner.dto;

/**
 * One member of a plan's roster.
 * <p>
 * The roster is the campaign's participants minus those who left, so this record
 * carries <em>all</em> participants and flags the ones who are out: a client can
 * then show "User3 left" with a way back in, instead of a name silently
 * disappearing from the list.
 *
 * @param userId         the member
 * @param nickname       their nickname in this campaign
 * @param role           their table role ({@code GM} or {@code PLAYER}) — carried
 *                       so the roster can mark who runs the table
 * @param excluded       true when they are not in the roster (left, or removed)
 * @param removedByOwner true when the campaign owner removed them; only the owner
 *                       puts them back, so a member cannot undo it themselves
 */
public record PlannerMemberDTO(
        Long userId,
        String nickname,
        String role,
        boolean excluded,
        boolean removedByOwner
) {
}
