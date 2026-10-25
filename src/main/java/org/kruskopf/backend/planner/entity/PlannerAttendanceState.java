package org.kruskopf.backend.planner.entity;

import org.kruskopf.backend.exception.InvalidRequestException;

import java.util.Locale;
import java.util.Optional;

/**
 * One member's answer to one candidate date.
 * <p>
 * There is deliberately no {@code UNANSWERED}: a member who has not answered has
 * no row at all. That is what makes "not answered" count against a date instead
 * of being indistinguishable from a maybe — the colour marker is only allowed to
 * go green when everybody still in the plan has said yes.
 * <p>
 * {@code MAYBE} is its own state: it shows in the roster so nobody is assumed
 * either way, and it is never counted as a yes.
 */
public enum PlannerAttendanceState {

    /** Can attend. */
    YES,

    /** Cannot attend. */
    NO,

    /** Not sure yet — visible, but never counted as an available member. */
    MAYBE;

    /**
     * @param rawState the answer as it arrived in a request body
     * @return the matching state
     * @throws InvalidRequestException when the value names no state (400, not a 500)
     */
    public static PlannerAttendanceState fromValue(String rawState) {
        if (rawState == null) {
            throw new InvalidRequestException("An attendance state is required (YES, NO or MAYBE)");
        }
        return Optional.of(rawState.trim().toUpperCase(Locale.ROOT))
                .filter(value -> value.equals(YES.name()) || value.equals(NO.name()) || value.equals(MAYBE.name()))
                .map(PlannerAttendanceState::valueOf)
                .orElseThrow(() -> new InvalidRequestException("Unknown attendance state: " + rawState));
    }
}
