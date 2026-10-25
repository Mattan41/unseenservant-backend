package org.kruskopf.backend.planner.dto;

/**
 * Marking whether the caller can attend one candidate date:
 * {@code YES}, {@code NO} or {@code MAYBE}.
 * <p>
 * Sent to {@code PUT .../attendance} — one answer per member per date, so a
 * second call with another value changes the answer rather than adding one.
 * {@code DELETE} on the same path withdraws it again, which is how a member goes
 * back to "not answered".
 */
public record PlannerAttendanceUpdateDTO(String state) {
}
