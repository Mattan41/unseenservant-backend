package org.kruskopf.backend.planner.dto;

/**
 * Deciding a plan or reopening it.
 * <p>
 * {@code status} is {@code DECIDED} or {@code OPEN}. Deciding requires
 * {@code decidedDateId} to name one of the plan's own dates; reopening clears the
 * choice, so the status and the date can never disagree. {@code version} is the
 * plan's token as it was loaded (409 when stale).
 */
public record PlannerStatusUpdateDTO(String status, Long decidedDateId, Long version) {
}
