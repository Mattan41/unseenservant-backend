package org.kruskopf.backend.planner.repository;

import org.kruskopf.backend.planner.entity.PlannerDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PlannerDateRepository extends JpaRepository<PlannerDate, Long> {

    List<PlannerDate> findByPlanIdOrderByDateAsc(Long planId);

    /** The dates of many plans in one query, for the planner's list view. */
    List<PlannerDate> findByPlanIdInOrderByDateAsc(Collection<Long> planIds);

    long countByPlanId(Long planId);
}
