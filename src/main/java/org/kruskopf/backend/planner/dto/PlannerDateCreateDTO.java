package org.kruskopf.backend.planner.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Offering a date. Any participant may add one; the day is required, the times
 * and the note are not.
 *
 * @param date      the candidate day
 * @param startTime optional start; {@code endTime} without it is refused
 * @param endTime   optional end
 * @param note      optional short note ("Sunday afternoon, bring dice")
 */
public record PlannerDateCreateDTO(LocalDate date, LocalTime startTime, LocalTime endTime, String note) {
}
