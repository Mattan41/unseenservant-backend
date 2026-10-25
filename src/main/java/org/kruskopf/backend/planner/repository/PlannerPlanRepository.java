package org.kruskopf.backend.planner.repository;

import org.kruskopf.backend.planner.entity.PlannerPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlannerPlanRepository extends JpaRepository<PlannerPlan, Long> {

    /** A campaign's plans: open ones first, then the decided history, newest first. */
    List<PlannerPlan> findByCampaignIdOrderByStatusAscCreatedAtDesc(Long campaignId);

    long countByCampaignId(Long campaignId);
}
