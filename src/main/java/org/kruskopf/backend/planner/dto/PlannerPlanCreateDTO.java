package org.kruskopf.backend.planner.dto;

/**
 * Starting a plan. Any participant may, so the body is only what a plan needs to
 * exist: a free-text title ("Session 12", "Weekend trip") and an optional
 * markdown description.
 */
public record PlannerPlanCreateDTO(String title, String description) {
}
