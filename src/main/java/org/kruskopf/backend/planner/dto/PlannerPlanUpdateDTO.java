package org.kruskopf.backend.planner.dto;

/**
 * Editing a plan's text. {@code version} is required — it is the token from the
 * plan as it was loaded, and a stale one is refused with 409.
 */
public record PlannerPlanUpdateDTO(String title, String description, Long version) {
}
