package org.kruskopf.backend.planner.controller;

import org.kruskopf.backend.planner.dto.PlannerAttendanceUpdateDTO;
import org.kruskopf.backend.planner.dto.PlannerDateCreateDTO;
import org.kruskopf.backend.planner.dto.PlannerDateUpdateDTO;
import org.kruskopf.backend.planner.dto.PlannerPlanCreateDTO;
import org.kruskopf.backend.planner.dto.PlannerPlanDTO;
import org.kruskopf.backend.planner.dto.PlannerPlanUpdateDTO;
import org.kruskopf.backend.planner.dto.PlannerStatusUpdateDTO;
import org.kruskopf.backend.planner.service.PlannerService;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The planner: plans, their candidate dates, and who can attend which.
 * <p>
 * Every endpoint is scoped to a campaign the caller is a participant of, and every
 * mutation answers with the whole plan — so the client always holds the new state
 * instead of guessing it. Who may do what (any participant for everything, the
 * campaign owner for moving somebody else in or out of the roster, a plan's creator
 * or the owner for deleting it) lives in {@link PlannerService}; this is the coarse
 * role gate plus the URL shape.
 * <p>
 * Paths say what they act on: {@code /plans/**} for a plan, {@code /dates/**} for
 * one candidate date, and {@code /plans/{id}/members/**} for its roster — the
 * caller's own seat under {@code /members/me}, and somebody else's under
 * {@code /members/{memberId}} (which is the campaign owner's, in both directions).
 * That separation is what keeps "leave this plan" and "remove that member" two clear
 * calls rather than one call whose meaning depends on its body.
 */
@RestController
@RequestMapping("/api/planner")
@PreAuthorize("hasRole('ROLE_USER')")
public class PlannerController {

    private final PlannerService plannerService;

    public PlannerController(PlannerService plannerService) {
        this.plannerService = plannerService;
    }

    // ---------- plans ----------

    @GetMapping("/campaign/{campaignId}/plans")
    public ResponseEntity<List<PlannerPlanDTO>> listPlans(
            @PathVariable Long campaignId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.listPlans(campaignId, userId));
    }

    @PostMapping("/campaign/{campaignId}/plans")
    public ResponseEntity<PlannerPlanDTO> createPlan(
            @PathVariable Long campaignId,
            @RequestBody(required = false) PlannerPlanCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(plannerService.createPlan(campaignId, dto, userId));
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<PlannerPlanDTO> getPlan(
            @PathVariable Long planId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.getPlan(planId, userId));
    }

    @PatchMapping("/plans/{planId}")
    public ResponseEntity<PlannerPlanDTO> updatePlan(
            @PathVariable Long planId,
            @RequestBody(required = false) PlannerPlanUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.updatePlan(planId, dto, userId));
    }

    @PatchMapping("/plans/{planId}/status")
    public ResponseEntity<PlannerPlanDTO> updateStatus(
            @PathVariable Long planId,
            @RequestBody(required = false) PlannerStatusUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.updateStatus(planId, dto, userId));
    }

    @DeleteMapping("/plans/{planId}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable Long planId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        plannerService.deletePlan(planId, userId);
        return ResponseEntity.noContent().build();
    }

    // ---------- candidate dates ----------

    @PostMapping("/plans/{planId}/dates")
    public ResponseEntity<PlannerPlanDTO> addDate(
            @PathVariable Long planId,
            @RequestBody(required = false) PlannerDateCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(plannerService.addDate(planId, dto, userId));
    }

    @PatchMapping("/dates/{dateId}")
    public ResponseEntity<PlannerPlanDTO> updateDate(
            @PathVariable Long dateId,
            @RequestBody(required = false) PlannerDateUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.updateDate(dateId, dto, userId));
    }

    @DeleteMapping("/dates/{dateId}")
    public ResponseEntity<PlannerPlanDTO> deleteDate(
            @PathVariable Long dateId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.deleteDate(dateId, userId));
    }

    // ---------- attendance ----------

    @PutMapping("/dates/{dateId}/attendance")
    public ResponseEntity<PlannerPlanDTO> setAttendance(
            @PathVariable Long dateId,
            @RequestBody(required = false) PlannerAttendanceUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.setAttendance(dateId, dto == null ? null : dto.state(), userId));
    }

    @DeleteMapping("/dates/{dateId}/attendance")
    public ResponseEntity<PlannerPlanDTO> clearAttendance(
            @PathVariable Long dateId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.clearAttendance(dateId, userId));
    }

    // ---------- the roster ----------

    @PutMapping("/plans/{planId}/members/me")
    public ResponseEntity<PlannerPlanDTO> rejoinPlan(
            @PathVariable Long planId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.rejoinPlan(planId, userId));
    }

    @DeleteMapping("/plans/{planId}/members/me")
    public ResponseEntity<PlannerPlanDTO> leavePlan(
            @PathVariable Long planId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.leavePlan(planId, userId));
    }

    @DeleteMapping("/plans/{planId}/members/{memberId}")
    public ResponseEntity<PlannerPlanDTO> removeMember(
            @PathVariable Long planId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.removeMember(planId, memberId, userId));
    }

    @PutMapping("/plans/{planId}/members/{memberId}")
    public ResponseEntity<PlannerPlanDTO> addMember(
            @PathVariable Long planId,
            @PathVariable Long memberId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(plannerService.addMember(planId, memberId, userId));
    }
}
