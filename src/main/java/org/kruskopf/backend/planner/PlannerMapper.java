package org.kruskopf.backend.planner;

import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.planner.dto.PlannerAttendanceDTO;
import org.kruskopf.backend.planner.dto.PlannerDateDTO;
import org.kruskopf.backend.planner.dto.PlannerMemberDTO;
import org.kruskopf.backend.planner.dto.PlannerPlanDTO;
import org.kruskopf.backend.planner.entity.PlannerAttendance;
import org.kruskopf.backend.planner.entity.PlannerDate;
import org.kruskopf.backend.planner.entity.PlannerMemberExclusion;
import org.kruskopf.backend.planner.entity.PlannerPlan;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps planner entities to their DTOs.
 * <p>
 * The pieces are passed in already loaded, which is what lets
 * {@code PlannerService} fetch a whole list of plans in a handful of queries
 * instead of one set per plan. Settings, ordering and the roster are the
 * service's business; a mapper that queried would hide all of it.
 */
@Component
public class PlannerMapper {

    public PlannerPlanDTO toPlanDTO(PlannerPlan plan,
                                    List<PlannerDateDTO> dates,
                                    List<PlannerMemberDTO> members) {
        return new PlannerPlanDTO(
                plan.getId(),
                plan.getCampaign() != null ? plan.getCampaign().getId() : null,
                plan.getTitle(),
                plan.getDescription(),
                plan.getStatus().name(),
                plan.getDecidedDate() != null ? plan.getDecidedDate().getId() : null,
                plan.getCreatedByUser() != null ? plan.getCreatedByUser().getId() : null,
                plan.getVersion(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                dates,
                members
        );
    }

    public PlannerDateDTO toDateDTO(PlannerDate date, List<PlannerAttendance> attendances) {
        return new PlannerDateDTO(
                date.getId(),
                date.getDate(),
                date.getStartTime(),
                date.getEndTime(),
                date.getNote(),
                date.getCreatedByUser() != null ? date.getCreatedByUser().getId() : null,
                date.getVersion(),
                attendances.stream()
                        .map(attendance -> new PlannerAttendanceDTO(
                                attendance.getUser().getId(),
                                attendance.getState().name()))
                        .toList()
        );
    }

    /**
     * One participant as a roster entry.
     *
     * @param exclusion the plan's opt-out row for them, or null when they are in
     */
    public PlannerMemberDTO toMemberDTO(CampaignUser participant, PlannerMemberExclusion exclusion) {
        return new PlannerMemberDTO(
                participant.getUser().getId(),
                participant.getNickname(),
                participant.getRole() != null ? participant.getRole().name() : null,
                exclusion != null,
                exclusion != null && exclusion.isRemovedByOwner()
        );
    }
}
