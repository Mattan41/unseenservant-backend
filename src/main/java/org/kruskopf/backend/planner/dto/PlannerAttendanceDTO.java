package org.kruskopf.backend.planner.dto;

/**
 * One member's answer to one candidate date: {@code YES}, {@code NO} or
 * {@code MAYBE}. A member with no entry has not answered yet.
 *
 * @param userId the member
 * @param state  the answer, by name
 */
public record PlannerAttendanceDTO(Long userId, String state) {
}
