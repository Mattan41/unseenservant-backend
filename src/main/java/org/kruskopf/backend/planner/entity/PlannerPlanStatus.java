package org.kruskopf.backend.planner.entity;

import org.kruskopf.backend.exception.InvalidRequestException;

import java.util.Locale;
import java.util.Optional;

/**
 * Where a plan is in its life.
 * <p>
 * Two states only. {@code OPEN} is a plan the group is still filling in: dates
 * get added, members vote, the roster changes. {@code DECIDED} is one they have
 * settled, which carries the chosen date and is read-only — the point of deciding
 * is that the answer stops moving. Reopening is the way back, because plans are
 * not a one-off: after the session the same plan can be reopened, or simply left
 * in the decided list as history.
 */
public enum PlannerPlanStatus {

    /** Being filled in: dates, votes and roster are all editable. */
    OPEN,

    /** Settled: carries the chosen date and refuses every change but reopening. */
    DECIDED;

    /** Whether a plan in this state still accepts changes. */
    public boolean isEditable() {
        return this == OPEN;
    }

    /**
     * @param rawStatus the status as it arrived in a request body
     * @return the matching status
     * @throws InvalidRequestException when the value names no status (400, not a 500)
     */
    public static PlannerPlanStatus fromValue(String rawStatus) {
        if (rawStatus == null) {
            throw new InvalidRequestException("A plan status is required (OPEN or DECIDED)");
        }
        return Optional.of(rawStatus.trim().toUpperCase(Locale.ROOT))
                .filter(value -> value.equals(OPEN.name()) || value.equals(DECIDED.name()))
                .map(PlannerPlanStatus::valueOf)
                .orElseThrow(() -> new InvalidRequestException("Unknown plan status: " + rawStatus));
    }
}
