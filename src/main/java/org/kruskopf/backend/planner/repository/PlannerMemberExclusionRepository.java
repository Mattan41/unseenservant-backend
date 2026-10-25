package org.kruskopf.backend.planner.repository;

import org.kruskopf.backend.planner.entity.PlannerMemberExclusion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlannerMemberExclusionRepository extends JpaRepository<PlannerMemberExclusion, Long> {

    List<PlannerMemberExclusion> findByPlanId(Long planId);

    List<PlannerMemberExclusion> findByPlanIdIn(Collection<Long> planIds);

    Optional<PlannerMemberExclusion> findByPlanIdAndUserId(Long planId, Long userId);
}
