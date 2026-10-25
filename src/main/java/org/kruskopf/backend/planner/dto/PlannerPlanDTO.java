package org.kruskopf.backend.planner.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A whole plan: its candidate dates, everybody's answers, and the roster.
 * <p>
 * Every planner endpoint that changes something answers with this, so a client
 * never has to guess what the change did to the rest of the plan — the same shape
 * the journal uses for its pages.
 *
 * @param decidedDateId the chosen date once decided, else null; always one of
 *                      {@code dates}
 * @param members       all campaign participants, each flagged if they are out
 */
public record PlannerPlanDTO(
        Long id,
        Long campaignId,
        String title,
        String description,
        String status,
        Long decidedDateId,
        Long createdByUserId,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<PlannerDateDTO> dates,
        List<PlannerMemberDTO> members
) {
}
