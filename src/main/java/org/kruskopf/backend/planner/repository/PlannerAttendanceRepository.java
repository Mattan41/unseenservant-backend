package org.kruskopf.backend.planner.repository;

import org.kruskopf.backend.planner.entity.PlannerAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlannerAttendanceRepository extends JpaRepository<PlannerAttendance, Long> {

    List<PlannerAttendance> findByPlanDateId(Long planDateId);

    /** Every answer of every date of a plan, in one query. */
    List<PlannerAttendance> findByPlanDateIdIn(Collection<Long> planDateIds);

    /** One member's answer to one date — the row a vote updates in place. */
    Optional<PlannerAttendance> findByPlanDateIdAndUserId(Long planDateId, Long userId);
}
