package org.kruskopf.backend.planner.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Correcting a date — the day, the times or the note. {@code version} is the
 * date's token as it was loaded (409 when stale).
 */
public record PlannerDateUpdateDTO(LocalDate date, LocalTime startTime, LocalTime endTime, String note, Long version) {
}
