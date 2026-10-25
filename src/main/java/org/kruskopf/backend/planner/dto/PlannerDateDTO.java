package org.kruskopf.backend.planner.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * One candidate date of a plan, with everybody's answer to it.
 * <p>
 * {@code attendances} holds one entry per member who has answered; a member who
 * has not is simply absent. The counts a colour marker needs are deliberately not
 * computed here — the client derives them from {@code attendances} and the plan's
 * {@code members} with the one implementation that also renders them, so the
 * threshold never exists twice.
 */
public record PlannerDateDTO(
        Long id,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String note,
        Long createdByUserId,
        long version,
        List<PlannerAttendanceDTO> attendances
) {
}
