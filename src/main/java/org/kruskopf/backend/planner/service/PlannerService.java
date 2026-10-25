package org.kruskopf.backend.planner.service;

import org.kruskopf.backend.addon.AddonKey;
import org.kruskopf.backend.addon.service.CampaignAddonService;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.ConcurrentUpdateException;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.exception.UniqueConstraintViolationException;
import org.kruskopf.backend.planner.PlannerMapper;
import org.kruskopf.backend.planner.dto.PlannerDateCreateDTO;
import org.kruskopf.backend.planner.dto.PlannerDateDTO;
import org.kruskopf.backend.planner.dto.PlannerDateUpdateDTO;
import org.kruskopf.backend.planner.dto.PlannerMemberDTO;
import org.kruskopf.backend.planner.dto.PlannerPlanCreateDTO;
import org.kruskopf.backend.planner.dto.PlannerPlanDTO;
import org.kruskopf.backend.planner.dto.PlannerPlanUpdateDTO;
import org.kruskopf.backend.planner.dto.PlannerStatusUpdateDTO;
import org.kruskopf.backend.planner.entity.PlannerAttendance;
import org.kruskopf.backend.planner.entity.PlannerAttendanceState;
import org.kruskopf.backend.planner.entity.PlannerDate;
import org.kruskopf.backend.planner.entity.PlannerMemberExclusion;
import org.kruskopf.backend.planner.entity.PlannerPlan;
import org.kruskopf.backend.planner.entity.PlannerPlanStatus;
import org.kruskopf.backend.planner.repository.PlannerAttendanceRepository;
import org.kruskopf.backend.planner.repository.PlannerDateRepository;
import org.kruskopf.backend.planner.repository.PlannerMemberExclusionRepository;
import org.kruskopf.backend.planner.repository.PlannerPlanRepository;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The campaign planner: a plan, its candidate dates, and who can attend which.
 * <p>
 * Three rules shape everything here:
 * <ul>
 *   <li><strong>It is the whole table's tool.</strong> Every participant may start
 *       a plan, offer dates, vote, decide and reopen — finding a date is not an
 *       administrative act. Only two things are restricted: moving <em>somebody
 *       else</em> out of a plan and back into it (the campaign owner), and deleting
 *       a plan (its creator, or the campaign owner).</li>
 *   <li><strong>The roster is implicit.</strong> It is the campaign's participants
 *       minus this plan's exclusion rows, so everybody is in a plan the moment it
 *       exists and nobody has to be added. A member who leaves — or is removed —
 *       gets one row; their answers stay, so rejoining restores them. That is why
 *       a member with no answer has no row at all, and why an unanswered date
 *       counts against it: green has to mean everybody said yes.</li>
 *   <li><strong>A decided plan stops moving.</strong> {@code DECIDED} refuses new
 *       dates, votes and roster changes until somebody reopens it, which is what
 *       makes saying "decided" worth anything. Every change answers with the whole
 *       plan, so one response always describes the new state.</li>
 * </ul>
 * The colour marker is deliberately <em>not</em> computed here: the DTO carries
 * the answers and the roster, and the client turns them into a percentage and a
 * colour with the single implementation that also renders it.
 */
@Service
public class PlannerService {

    /** The add-on this service belongs to; its endpoints are closed while it is off. */
    private static final AddonKey PLANNER_ADDON = AddonKey.PLANNER;

    private final PlannerPlanRepository planRepository;
    private final PlannerDateRepository dateRepository;
    private final PlannerAttendanceRepository attendanceRepository;
    private final PlannerMemberExclusionRepository exclusionRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignUserRepository campaignUserRepository;
    private final UserRepository userRepository;
    private final CampaignPermissionService campaignPermissionService;
    private final CampaignAddonService campaignAddonService;
    private final PlannerMapper plannerMapper;

    private final int maxTitleLength;
    private final int maxDescriptionLength;
    private final int maxNoteLength;
    private final int maxPlansPerCampaign;
    private final int maxDatesPerPlan;

    public PlannerService(PlannerPlanRepository planRepository,
                          PlannerDateRepository dateRepository,
                          PlannerAttendanceRepository attendanceRepository,
                          PlannerMemberExclusionRepository exclusionRepository,
                          CampaignRepository campaignRepository,
                          CampaignUserRepository campaignUserRepository,
                          UserRepository userRepository,
                          CampaignPermissionService campaignPermissionService,
                          CampaignAddonService campaignAddonService,
                          PlannerMapper plannerMapper,
                          @Value("${app.planner.max-title-length:200}") int maxTitleLength,
                          @Value("${app.planner.max-description-length:10000}") int maxDescriptionLength,
                          @Value("${app.planner.max-note-length:500}") int maxNoteLength,
                          @Value("${app.planner.max-plans-per-campaign:100}") int maxPlansPerCampaign,
                          @Value("${app.planner.max-dates-per-plan:50}") int maxDatesPerPlan) {
        this.planRepository = planRepository;
        this.dateRepository = dateRepository;
        this.attendanceRepository = attendanceRepository;
        this.exclusionRepository = exclusionRepository;
        this.campaignRepository = campaignRepository;
        this.campaignUserRepository = campaignUserRepository;
        this.userRepository = userRepository;
        this.campaignPermissionService = campaignPermissionService;
        this.campaignAddonService = campaignAddonService;
        this.plannerMapper = plannerMapper;
        this.maxTitleLength = maxTitleLength;
        this.maxDescriptionLength = maxDescriptionLength;
        this.maxNoteLength = maxNoteLength;
        this.maxPlansPerCampaign = maxPlansPerCampaign;
        this.maxDatesPerPlan = maxDatesPerPlan;
    }

    // ---------- reads ----------

    /**
     * A campaign's plans: the open ones first, then the decided history, newest
     * first. The list is built from a handful of queries for the whole page, not
     * one set per plan.
     */
    @Transactional(readOnly = true)
    public List<PlannerPlanDTO> listPlans(Long campaignId, long userId) {
        requireCampaign(campaignId);
        requireAddonEnabled(campaignId);
        requireParticipant(campaignId, userId);

        List<PlannerPlan> plans = planRepository.findByCampaignIdOrderByStatusAscCreatedAtDesc(campaignId);
        List<CampaignUser> participants = campaignUserRepository.findByCampaignId(campaignId);

        List<Long> planIds = plans.stream().map(PlannerPlan::getId).toList();
        List<PlannerDate> dates = datesOf(planIds);
        Map<Long, List<PlannerAttendance>> attendancesByDate = attendancesOf(dates);
        Map<Long, List<PlannerMemberExclusion>> exclusionsByPlan = exclusionsOf(planIds);
        Map<Long, List<PlannerDate>> datesByPlan = dates.stream()
                .collect(Collectors.groupingBy(date -> date.getPlan().getId()));

        return plans.stream()
                .map(plan -> plannerMapper.toPlanDTO(
                        plan,
                        toDateDTOs(datesByPlan.getOrDefault(plan.getId(), List.of()), attendancesByDate),
                        toMemberDTOs(participants, exclusionsByPlan.getOrDefault(plan.getId(), List.of()))))
                .toList();
    }

    /** One plan, whole: its dates, everybody's answers and the roster. */
    @Transactional(readOnly = true)
    public PlannerPlanDTO getPlan(Long planId, long userId) {
        return toPlanDTO(loadPlanForParticipant(planId, userId));
    }

    // ---------- assembling a plan ----------

    private PlannerPlanDTO toPlanDTO(PlannerPlan plan) {
        List<PlannerDate> dates = dateRepository.findByPlanIdOrderByDateAsc(plan.getId());
        return plannerMapper.toPlanDTO(
                plan,
                toDateDTOs(dates, attendancesOf(dates)),
                toMemberDTOs(campaignUserRepository.findByCampaignId(plan.getCampaign().getId()),
                        exclusionRepository.findByPlanId(plan.getId())));
    }

    private List<PlannerDate> datesOf(List<Long> planIds) {
        return planIds.isEmpty() ? List.of() : dateRepository.findByPlanIdInOrderByDateAsc(planIds);
    }

    private Map<Long, List<PlannerAttendance>> attendancesOf(List<PlannerDate> dates) {
        List<Long> dateIds = dates.stream().map(PlannerDate::getId).toList();
        if (dateIds.isEmpty()) {
            return Map.of();
        }
        return attendanceRepository.findByPlanDateIdIn(dateIds).stream()
                .collect(Collectors.groupingBy(attendance -> attendance.getPlanDate().getId()));
    }

    private Map<Long, List<PlannerMemberExclusion>> exclusionsOf(List<Long> planIds) {
        if (planIds.isEmpty()) {
            return Map.of();
        }
        return exclusionRepository.findByPlanIdIn(planIds).stream()
                .collect(Collectors.groupingBy(exclusion -> exclusion.getPlan().getId()));
    }

    private List<PlannerDateDTO> toDateDTOs(List<PlannerDate> dates,
                                            Map<Long, List<PlannerAttendance>> attendancesByDate) {
        return dates.stream()
                .map(date -> plannerMapper.toDateDTO(date,
                        attendancesByDate.getOrDefault(date.getId(), List.of())))
                .toList();
    }

    /**
     * The roster: every campaign participant, each flagged when this plan has them
     * out. Sorted by nickname so the list does not jump around between loads.
     */
    private List<PlannerMemberDTO> toMemberDTOs(List<CampaignUser> participants,
                                                List<PlannerMemberExclusion> exclusions) {
        Map<Long, PlannerMemberExclusion> byUser = exclusions.stream()
                .collect(Collectors.toMap(exclusion -> exclusion.getUser().getId(),
                        Function.identity(), (first, second) -> first));

        return participants.stream()
                .map(participant -> plannerMapper.toMemberDTO(participant, byUser.get(participant.getUser().getId())))
                .sorted(Comparator.comparing(PlannerMemberDTO::nickname,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    // ---------- the plan itself ----------

    /**
     * Start a plan. Any participant may, because finding a date is not an
     * administrative act — and the roster is implicit, so this one call is all it
     * takes for everybody to be in it.
     */
    @Transactional
    public PlannerPlanDTO createPlan(Long campaignId, PlannerPlanCreateDTO dto, long userId) {
        Campaign campaign = requireCampaign(campaignId);
        requireAddonEnabled(campaignId);
        requireParticipant(campaignId, userId);

        String title = requireTitle(dto == null ? null : dto.title());
        String description = requireDescription(dto == null ? null : dto.description());

        if (planRepository.countByCampaignId(campaignId) >= maxPlansPerCampaign) {
            throw new InvalidRequestException(
                    "A campaign may hold at most " + maxPlansPerCampaign + " plans");
        }

        PlannerPlan plan = planRepository.saveAndFlush(
                new PlannerPlan(campaign, requireUser(userId), title, description));

        return toPlanDTO(plan);
    }

    /** Edit a plan's title or description. Any participant, while it is open. */
    @Transactional
    public PlannerPlanDTO updatePlan(Long planId, PlannerPlanUpdateDTO dto, long userId) {
        PlannerPlan plan = loadPlanForParticipant(planId, userId);
        requireOpen(plan);
        requireVersion(plan.getVersion(), dto == null ? null : dto.version(), "plan");

        plan.setTitle(requireTitle(dto == null ? null : dto.title()));
        plan.setDescription(requireDescription(dto == null ? null : dto.description()));

        return toPlanDTO(planRepository.saveAndFlush(plan));
    }

    /**
     * Decide a plan, or reopen it.
     * <p>
     * Deciding takes one of the plan's own dates and is only possible while the
     * plan is open — a settled plan has to be reopened first, so a decision cannot
     * be changed by accident. Reopening clears the chosen date, which is what keeps
     * {@code status} and {@code decidedDate} from ever disagreeing.
     */
    @Transactional
    public PlannerPlanDTO updateStatus(Long planId, PlannerStatusUpdateDTO dto, long userId) {
        PlannerPlan plan = loadPlanForParticipant(planId, userId);
        requireVersion(plan.getVersion(), dto == null ? null : dto.version(), "plan");

        PlannerPlanStatus status = PlannerPlanStatus.fromValue(dto == null ? null : dto.status());
        if (status == PlannerPlanStatus.DECIDED) {
            requireOpen(plan);
            plan.setDecidedDate(requireOwnDate(plan, dto.decidedDateId()));
        } else {
            plan.setDecidedDate(null);
        }
        plan.setStatus(status);

        return toPlanDTO(planRepository.saveAndFlush(plan));
    }

    /**
     * Delete a plan, with its dates and answers. Its creator or the campaign owner
     * may — anything else would let one participant throw away work the others are
     * relying on.
     */
    @Transactional
    public void deletePlan(Long planId, long userId) {
        PlannerPlan plan = loadPlanForParticipant(planId, userId);

        boolean isCreator = plan.getCreatedByUser() != null
                && plan.getCreatedByUser().getId().equals(userId);
        if (!isCreator && !campaignPermissionService.isCampaignOwner(plan.getCampaign().getId(), userId)) {
            throw new UnauthorizedAccessException(
                    "Only the plan's creator or the campaign owner can delete a plan");
        }

        planRepository.delete(plan);
    }

    // ---------- candidate dates ----------

    /** Offer a date. Any participant, up to the plan's limit. */
    @Transactional
    public PlannerPlanDTO addDate(Long planId, PlannerDateCreateDTO dto, long userId) {
        PlannerPlan plan = loadPlanForParticipant(planId, userId);
        requireOpen(plan);

        LocalDate day = requireDate(dto == null ? null : dto.date());
        LocalTime start = dto == null ? null : dto.startTime();
        LocalTime end = dto == null ? null : dto.endTime();
        requireTimeRange(start, end);

        if (dateRepository.countByPlanId(planId) >= maxDatesPerPlan) {
            throw new InvalidRequestException("A plan may hold at most " + maxDatesPerPlan + " dates");
        }
        requireUnusedDate(planId, day, null);

        PlannerDate date = new PlannerDate(plan, requireUser(userId), day);
        date.setStartTime(start);
        date.setEndTime(end);
        date.setNote(requireNote(dto == null ? null : dto.note()));
        dateRepository.saveAndFlush(date);

        return toPlanDTO(plan);
    }

    /**
     * Correct a date — the day, the times or the note. The member who offered it,
     * the plan's creator and the campaign owner may.
     */
    @Transactional
    public PlannerPlanDTO updateDate(Long dateId, PlannerDateUpdateDTO dto, long userId) {
        PlannerDate date = loadDateForParticipant(dateId, userId);
        PlannerPlan plan = date.getPlan();
        requireOpen(plan);
        requireDateEditable(date, userId);
        requireVersion(date.getVersion(), dto == null ? null : dto.version(), "date");

        LocalDate day = requireDate(dto == null ? null : dto.date());
        LocalTime start = dto == null ? null : dto.startTime();
        LocalTime end = dto == null ? null : dto.endTime();
        requireTimeRange(start, end);
        requireUnusedDate(plan.getId(), day, date.getId());

        date.setDate(day);
        date.setStartTime(start);
        date.setEndTime(end);
        date.setNote(requireNote(dto == null ? null : dto.note()));
        dateRepository.saveAndFlush(date);

        return toPlanDTO(plan);
    }

    /**
     * Withdraw a date, with the answers given for it. A plan that had decided on
     * it stays decided but loses the date — the same thing the schema does
     * ({@code ON DELETE SET NULL}).
     */
    @Transactional
    public PlannerPlanDTO deleteDate(Long dateId, long userId) {
        PlannerDate date = loadDateForParticipant(dateId, userId);
        PlannerPlan plan = date.getPlan();
        requireOpen(plan);
        requireDateEditable(date, userId);

        if (plan.getDecidedDate() != null && plan.getDecidedDate().getId().equals(date.getId())) {
            plan.setDecidedDate(null);
            planRepository.saveAndFlush(plan);
        }
        dateRepository.delete(date);
        dateRepository.flush();

        return toPlanDTO(plan);
    }

    // ---------- attendance ----------

    /** Answer a date: YES, NO or MAYBE. One answer per member, changed in place. */
    @Transactional
    public PlannerPlanDTO setAttendance(Long dateId, String rawState, long userId) {
        PlannerDate date = loadDateForParticipant(dateId, userId);
        PlannerPlan plan = date.getPlan();
        requireOpen(plan);
        requireRosterMember(plan, userId);

        PlannerAttendanceState state = PlannerAttendanceState.fromValue(rawState);
        PlannerAttendance attendance = attendanceRepository
                .findByPlanDateIdAndUserId(dateId, userId)
                .orElseGet(() -> new PlannerAttendance(date, requireUser(userId), state));
        attendance.setState(state);
        attendanceRepository.saveAndFlush(attendance);

        return toPlanDTO(plan);
    }

    /** Withdraw an answer, which puts the member back to "not answered". */
    @Transactional
    public PlannerPlanDTO clearAttendance(Long dateId, long userId) {
        PlannerDate date = loadDateForParticipant(dateId, userId);
        PlannerPlan plan = date.getPlan();
        requireOpen(plan);

        attendanceRepository.findByPlanDateIdAndUserId(dateId, userId)
                .ifPresent(attendanceRepository::delete);

        return toPlanDTO(plan);
    }

    // ---------- the roster ----------

    /** Leave a plan: one row, and the member is out. Their answers stay. */
    @Transactional
    public PlannerPlanDTO leavePlan(Long planId, long userId) {
        PlannerPlan plan = loadPlanForParticipant(planId, userId);
        requireOpen(plan);

        if (exclusionRepository.findByPlanIdAndUserId(planId, userId).isEmpty()) {
            User member = requireUser(userId);
            exclusionRepository.saveAndFlush(
                    new PlannerMemberExclusion(plan, member, member.getUserName(), false));
        }

        return toPlanDTO(plan);
    }

    /**
     * Rejoin a plan the member left. Refused when the campaign owner is the one
     * who took them out — otherwise the owner's control would be worth nothing.
     */
    @Transactional
    public PlannerPlanDTO rejoinPlan(Long planId, long userId) {
        PlannerPlan plan = loadPlanForParticipant(planId, userId);
        requireOpen(plan);

        Optional<PlannerMemberExclusion> exclusion = exclusionRepository.findByPlanIdAndUserId(planId, userId);
        if (exclusion.isPresent()) {
            if (exclusion.get().isRemovedByOwner()) {
                throw new UnauthorizedAccessException(
                        "The campaign owner removed you from this plan; only they can put you back");
            }
            exclusionRepository.delete(exclusion.get());
        }

        return toPlanDTO(plan);
    }

    /**
     * Put a participant back into a plan — the other half of the owner's removal,
     * and the only way back for somebody the owner took out (their own rejoin is
     * refused for exactly that reason). Their exclusion row goes; their answers were
     * never touched, so they count again at once.
     * <p>
     * Like {@link #leavePlan} and {@link #rejoinPlan}, the call is idempotent:
     * putting back a member who is not out changes nothing rather than failing, so
     * the owner never has to wonder whether somebody beat them to it.
     */
    @Transactional
    public PlannerPlanDTO addMember(Long planId, Long memberId, long userId) {
        PlannerPlan plan = loadPlanForParticipant(planId, userId);
        requireOpen(plan);
        requireCampaignOwner(plan, userId);
        requireParticipant(plan.getCampaign().getId(), memberId);

        exclusionRepository.findByPlanIdAndUserId(planId, memberId)
                .ifPresent(exclusionRepository::delete);

        return toPlanDTO(plan);
    }

    /**
     * Remove a participant from a plan — the campaign owner's one exclusive right
     * here. Removing themselves is a member leaving, so it stays undoable.
     */
    @Transactional
    public PlannerPlanDTO removeMember(Long planId, Long memberId, long userId) {
        PlannerPlan plan = loadPlanForParticipant(planId, userId);
        requireOpen(plan);
        requireCampaignOwner(plan, userId);
        requireParticipant(plan.getCampaign().getId(), memberId);

        boolean selfRemoval = memberId.equals(userId);
        User actor = requireUser(userId);

        PlannerMemberExclusion exclusion = exclusionRepository
                .findByPlanIdAndUserId(planId, memberId)
                .orElseGet(() -> new PlannerMemberExclusion(plan, requireUser(memberId), actor.getUserName(), false));
        exclusion.setRemovedBy(actor.getUserName());
        exclusion.setRemovedByOwner(!selfRemoval);
        exclusionRepository.saveAndFlush(exclusion);

        return toPlanDTO(plan);
    }

    // ---------- loading and the rules ----------

    private PlannerPlan loadPlanForParticipant(Long planId, long userId) {
        PlannerPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + planId));

        requireAddonEnabled(plan.getCampaign().getId());
        requireParticipant(plan.getCampaign().getId(), userId);
        return plan;
    }

    private PlannerDate loadDateForParticipant(Long dateId, long userId) {
        PlannerDate date = dateRepository.findById(dateId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan date not found with id: " + dateId));

        requireAddonEnabled(date.getPlan().getCampaign().getId());
        requireParticipant(date.getPlan().getCampaign().getId(), userId);
        return date;
    }

    /** A campaign whose planner has been switched off keeps its endpoints closed. */
    private void requireAddonEnabled(Long campaignId) {
        if (!campaignAddonService.isEnabled(campaignId, PLANNER_ADDON)) {
            throw new InvalidRequestException("The planner is not enabled for this campaign");
        }
    }

    private void requireParticipant(Long campaignId, long userId) {
        if (!campaignPermissionService.isParticipant(campaignId, userId)) {
            throw new UnauthorizedAccessException("User is not a participant in this campaign");
        }
    }

    private void requireParticipant(Long campaignId, Long userId) {
        if (userId == null) {
            throw new InvalidRequestException("A member id is required");
        }
        requireParticipant(campaignId, userId.longValue());
    }

    private void requireCampaignOwner(PlannerPlan plan, long userId) {
        if (!campaignPermissionService.isCampaignOwner(plan.getCampaign().getId(), userId)) {
            throw new UnauthorizedAccessException(
                    "Only the campaign owner can move another member in or out of a plan");
        }
    }

    /** The plan is settled: it refuses everything but reopening. */
    private void requireOpen(PlannerPlan plan) {
        if (!plan.getStatus().isEditable()) {
            throw new InvalidRequestException(
                    "This plan is decided. Reopen it before changing dates, answers or the roster.");
        }
    }

    private void requireRosterMember(PlannerPlan plan, long userId) {
        if (exclusionRepository.findByPlanIdAndUserId(plan.getId(), userId).isPresent()) {
            throw new UnauthorizedAccessException("You are not in this plan. Rejoin it before answering.");
        }
    }

    private void requireDateEditable(PlannerDate date, long userId) {
        boolean isCreator = date.getCreatedByUser() != null
                && date.getCreatedByUser().getId().equals(userId);
        boolean isPlanCreator = date.getPlan().getCreatedByUser() != null
                && date.getPlan().getCreatedByUser().getId().equals(userId);
        boolean isOwner = campaignPermissionService
                .isCampaignOwner(date.getPlan().getCampaign().getId(), userId);

        if (!isCreator && !isPlanCreator && !isOwner) {
            throw new UnauthorizedAccessException(
                    "Only the member who offered this date, the plan's creator or the campaign owner can change it");
        }
    }

    /** One of the plan's own dates — a decision cannot point outside the plan. */
    private PlannerDate requireOwnDate(PlannerPlan plan, Long decidedDateId) {
        if (decidedDateId == null) {
            throw new InvalidRequestException("A decided plan needs one of its own dates");
        }
        return dateRepository.findByPlanIdOrderByDateAsc(plan.getId()).stream()
                .filter(date -> date.getId().equals(decidedDateId))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Date " + decidedDateId + " is not a date of this plan"));
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));
    }

    private User requireUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    /**
     * The client's optimistic-concurrency token. Required — a missing token would
     * silently skip the check — and compared against the freshly loaded value.
     */
    private static void requireVersion(long current, Long offered, String subject) {
        if (offered == null) {
            throw new InvalidRequestException("A version is required to change a " + subject);
        }
        if (offered != current) {
            throw new ConcurrentUpdateException(
                    "This " + subject + " was updated by someone else. Reload and try again.");
        }
    }

    private String requireTitle(String raw) {
        String title = raw == null ? "" : raw.trim();
        if (title.isEmpty()) {
            throw new InvalidRequestException("A plan needs a title");
        }
        if (title.length() > maxTitleLength) {
            throw new InvalidRequestException("A plan title may be at most " + maxTitleLength + " characters");
        }
        return title;
    }

    private String requireDescription(String raw) {
        String description = raw == null ? "" : raw;
        if (description.length() > maxDescriptionLength) {
            throw new InvalidRequestException(
                    "A plan description may be at most " + maxDescriptionLength + " characters");
        }
        return description;
    }

    private String requireNote(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.length() > maxNoteLength) {
            throw new InvalidRequestException("A date note may be at most " + maxNoteLength + " characters");
        }
        return raw;
    }

    private static LocalDate requireDate(LocalDate date) {
        if (date == null) {
            throw new InvalidRequestException("A date is required");
        }
        return date;
    }

    private static void requireTimeRange(LocalTime start, LocalTime end) {
        if (end != null && start == null) {
            throw new InvalidRequestException("An end time needs a start time");
        }
        if (start != null && end != null && !end.isAfter(start)) {
            throw new InvalidRequestException("An end time has to be after the start time");
        }
    }

    /**
     * One row per day, which is what keeps a day's answers that day's answers. The
     * row being edited is skipped, so saving a date without changing the day works.
     */
    private void requireUnusedDate(Long planId, LocalDate date, Long editedDateId) {
        boolean takenByAnother = dateRepository.findByPlanIdOrderByDateAsc(planId).stream()
                .anyMatch(existing -> existing.getDate().equals(date)
                        && !existing.getId().equals(editedDateId));

        if (takenByAnother) {
            throw new UniqueConstraintViolationException("date", date.toString());
        }
    }
}
