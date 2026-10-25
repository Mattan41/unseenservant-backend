package org.kruskopf.backend.planner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.addon.AddonKey;
import org.kruskopf.backend.addon.service.CampaignAddonService;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.ConcurrentUpdateException;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.exception.UniqueConstraintViolationException;
import org.kruskopf.backend.planner.PlannerMapper;
import org.kruskopf.backend.planner.dto.PlannerDateCreateDTO;
import org.kruskopf.backend.planner.dto.PlannerDateUpdateDTO;
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
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rules that live in {@link PlannerService}: who may do what, what a decided
 * plan refuses, when a day is already offered, and the two ways a member can be
 * out of a roster.
 * <p>
 * The colour marker is not tested here on purpose — it is computed on the client,
 * from the answers and the roster this service hands over.
 */
@DisplayName("PlannerService")
class PlannerServiceTest {

    private static final long CAMPAIGN_ID = 1L;
    private static final long PLAN_ID = 4L;
    private static final long DATE_ID = 9L;
    private static final long USER_ID = 7L;
    private static final long OTHER_ID = 8L;
    private static final LocalDate A_DAY = LocalDate.of(2026, 3, 14);

    private PlannerPlanRepository planRepository;
    private PlannerDateRepository dateRepository;
    private PlannerAttendanceRepository attendanceRepository;
    private PlannerMemberExclusionRepository exclusionRepository;
    private CampaignRepository campaignRepository;
    private CampaignUserRepository campaignUserRepository;
    private UserRepository userRepository;
    private CampaignPermissionService campaignPermissionService;
    private CampaignAddonService campaignAddonService;

    private PlannerService service;

    private User user;
    private User other;
    private Campaign campaign;
    private PlannerPlan plan;

    @BeforeEach
    void setUp() {
        planRepository = mock(PlannerPlanRepository.class);
        dateRepository = mock(PlannerDateRepository.class);
        attendanceRepository = mock(PlannerAttendanceRepository.class);
        exclusionRepository = mock(PlannerMemberExclusionRepository.class);
        campaignRepository = mock(CampaignRepository.class);
        campaignUserRepository = mock(CampaignUserRepository.class);
        userRepository = mock(UserRepository.class);
        campaignPermissionService = mock(CampaignPermissionService.class);
        campaignAddonService = mock(CampaignAddonService.class);

        service = new PlannerService(planRepository, dateRepository, attendanceRepository, exclusionRepository,
                campaignRepository, campaignUserRepository, userRepository, campaignPermissionService,
                campaignAddonService, new PlannerMapper(), 200, 10_000, 500, 100, 50);

        user = userWithId(USER_ID, "User7");
        other = userWithId(OTHER_ID, "User8");
        campaign = campaignWithId(CAMPAIGN_ID, user);
        plan = planWithId(PLAN_ID, campaign, user);
    }

    // --- fixtures -------------------------------------------------------------

    private static User userWithId(long id, String name) {
        User entity = TestDataFactory.aUser(name);
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static Campaign campaignWithId(long id, User owner) {
        Campaign entity = TestDataFactory.aCampaign(owner);
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static PlannerPlan planWithId(long id, Campaign campaign, User creator) {
        PlannerPlan entity = new PlannerPlan(campaign, creator, "Session 12", "");
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static PlannerDate dateWithId(long id, PlannerPlan plan, User creator, LocalDate day) {
        PlannerDate entity = new PlannerDate(plan, creator, day);
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static CampaignUser participantOf(Campaign campaign, User member, String nickname) {
        return new CampaignUser(campaign, member, CampaignRole.PLAYER, nickname);
    }

    /** Enables the add-on, so a plan can be reached at all. */
    private void givenAddonEnabled() {
        when(campaignAddonService.isEnabled(CAMPAIGN_ID, AddonKey.PLANNER)).thenReturn(true);
    }

    /**
     * An open plan the caller may change: the add-on is on, they are a participant
     * with one candidate date, and nobody has answered yet.
     */
    private List<PlannerDate> givenOpenPlan() {
        List<PlannerDate> dates = List.of(dateWithId(DATE_ID, plan, user, A_DAY));

        givenAddonEnabled();
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(dateRepository.findByPlanIdOrderByDateAsc(PLAN_ID)).thenReturn(dates);
        when(dateRepository.findById(DATE_ID)).thenReturn(Optional.of(dates.get(0)));
        when(campaignUserRepository.findByCampaignId(CAMPAIGN_ID))
                .thenReturn(List.of(participantOf(campaign, user, "User7")));
        when(exclusionRepository.findByPlanId(PLAN_ID)).thenReturn(List.of());
        when(attendanceRepository.findByPlanDateIdIn(any())).thenReturn(List.of());
        return dates;
    }

    /** The plan comes back whole after a change, so saving has to return it. */
    private void givenSavingWorks() {
        when(planRepository.saveAndFlush(any(PlannerPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(dateRepository.saveAndFlush(any(PlannerDate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- who may do what ------------------------------------------------------

    @Test
    @DisplayName("a caller who is not a participant cannot start a plan")
    void createPlanRefusesANonParticipant() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        givenAddonEnabled();
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.createPlan(CAMPAIGN_ID,
                new PlannerPlanCreateDTO("Session 12", null), USER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
        verify(planRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("the planner's endpoints are closed while the add-on is off")
    void endpointsAreClosedWhileTheAddonIsOff() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        when(campaignAddonService.isEnabled(CAMPAIGN_ID, AddonKey.PLANNER)).thenReturn(false);

        assertThatThrownBy(() -> service.createPlan(CAMPAIGN_ID,
                new PlannerPlanCreateDTO("Session 12", null), USER_ID))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not enabled for this campaign");
    }

    @Test
    @DisplayName("a plan needs a title, and it is trimmed")
    void aPlanNeedsATitle() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        givenAddonEnabled();
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        givenSavingWorks();

        assertThatThrownBy(() -> service.createPlan(CAMPAIGN_ID,
                new PlannerPlanCreateDTO("   ", null), USER_ID))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("needs a title");

        PlannerPlanDTO created = service.createPlan(CAMPAIGN_ID,
                new PlannerPlanCreateDTO("  Session 12  ", null), USER_ID);

        assertThat(created.title()).isEqualTo("Session 12");
        assertThat(created.status()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("deleting a plan is for its creator or the campaign owner")
    void deletePlanIsForTheCreatorOrTheOwner() {
        PlannerPlan someoneElses = planWithId(PLAN_ID, campaign, other);
        givenAddonEnabled();
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(someoneElses));

        assertThatThrownBy(() -> service.deletePlan(PLAN_ID, USER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
        verify(planRepository, never()).delete(any(PlannerPlan.class));

        when(campaignPermissionService.isCampaignOwner(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        service.deletePlan(PLAN_ID, USER_ID);

        verify(planRepository).delete(someoneElses);
    }

    // --- optimistic locking and freezing --------------------------------------

    @Test
    @DisplayName("a stale or missing version token is refused")
    void aStaleVersionIsRefused() {
        givenOpenPlan();
        plan.setVersion(3);
        givenSavingWorks();

        assertThatThrownBy(() -> service.updatePlan(PLAN_ID,
                new PlannerPlanUpdateDTO("New title", null, 2L), USER_ID))
                .isInstanceOf(ConcurrentUpdateException.class);
        assertThatThrownBy(() -> service.updatePlan(PLAN_ID,
                new PlannerPlanUpdateDTO("New title", null, null), USER_ID))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("version is required");

        verify(planRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("a decided plan refuses new dates, and reopening clears the chosen date")
    void aDecidedPlanIsFrozen() {
        List<PlannerDate> dates = givenOpenPlan();
        givenSavingWorks();
        plan.setStatus(PlannerPlanStatus.DECIDED);
        plan.setDecidedDate(dates.get(0));

        assertThatThrownBy(() -> service.addDate(PLAN_ID,
                new PlannerDateCreateDTO(LocalDate.of(2026, 3, 21), null, null, null), USER_ID))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Reopen it");
        verify(dateRepository, never()).saveAndFlush(any(PlannerDate.class));

        PlannerPlanDTO reopened = service.updateStatus(PLAN_ID,
                new PlannerStatusUpdateDTO("OPEN", null, 0L), USER_ID);

        assertThat(reopened.status()).isEqualTo("OPEN");
        assertThat(reopened.decidedDateId()).isNull();
        assertThat(plan.getDecidedDate()).isNull();
    }

    @Test
    @DisplayName("deciding takes one of the plan's own dates")
    void decidingTakesOneOfThePlansOwnDates() {
        givenOpenPlan();
        givenSavingWorks();

        assertThatThrownBy(() -> service.updateStatus(PLAN_ID,
                new PlannerStatusUpdateDTO("DECIDED", null, 0L), USER_ID))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("one of its own dates");
        assertThatThrownBy(() -> service.updateStatus(PLAN_ID,
                new PlannerStatusUpdateDTO("DECIDED", 999L, 0L), USER_ID))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("is not a date of this plan");

        PlannerPlanDTO decided = service.updateStatus(PLAN_ID,
                new PlannerStatusUpdateDTO("DECIDED", DATE_ID, 0L), USER_ID);

        assertThat(decided.status()).isEqualTo("DECIDED");
        assertThat(decided.decidedDateId()).isEqualTo(DATE_ID);
    }

    // --- what a plan accepts --------------------------------------------------

    @Test
    @DisplayName("the same day cannot be offered twice in one plan")
    void theSameDayCannotBeOfferedTwice() {
        givenOpenPlan();

        assertThatThrownBy(() -> service.addDate(PLAN_ID,
                new PlannerDateCreateDTO(A_DAY, null, null, null), USER_ID))
                .isInstanceOf(UniqueConstraintViolationException.class);

        verify(dateRepository, never()).saveAndFlush(any(PlannerDate.class));
    }

    @Test
    @DisplayName("saving a date without changing its day is allowed")
    void savingADateOnItsOwnDayIsAllowed() {
        List<PlannerDate> dates = givenOpenPlan();
        givenSavingWorks();

        service.updateDate(DATE_ID, new PlannerDateUpdateDTO(
                A_DAY, LocalTime.of(18, 0), LocalTime.of(22, 0), "Bring dice", 0L), USER_ID);

        assertThat(dates.get(0).getNote()).isEqualTo("Bring dice");
        assertThat(dates.get(0).getStartTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    @DisplayName("an end time needs a start, and has to be after it")
    void anEndTimeNeedsAStartAndHasToBeAfterIt() {
        givenOpenPlan();
        LocalDate anotherDay = LocalDate.of(2026, 3, 21);

        assertThatThrownBy(() -> service.addDate(PLAN_ID,
                new PlannerDateCreateDTO(anotherDay, null, LocalTime.of(22, 0), null), USER_ID))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("end time needs a start time");
        assertThatThrownBy(() -> service.addDate(PLAN_ID,
                new PlannerDateCreateDTO(anotherDay, LocalTime.of(22, 0), LocalTime.of(18, 0), null), USER_ID))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("has to be after the start time");
    }

    // --- answers --------------------------------------------------------------

    @Test
    @DisplayName("an unknown answer is refused")
    void anUnknownAnswerIsRefused() {
        givenOpenPlan();

        assertThatThrownBy(() -> service.setAttendance(DATE_ID, "probably", USER_ID))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unknown attendance state");
    }

    @Test
    @DisplayName("a member who left the plan cannot answer")
    void aMemberWhoLeftCannotAnswer() {
        givenOpenPlan();
        when(exclusionRepository.findByPlanIdAndUserId(PLAN_ID, USER_ID))
                .thenReturn(Optional.of(new PlannerMemberExclusion(plan, user, "User7", false)));

        assertThatThrownBy(() -> service.setAttendance(DATE_ID, "yes", USER_ID))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("not in this plan");
    }

    @Test
    @DisplayName("an answer is stored, changed in place, and withdrawn")
    void anAnswerIsStoredChangedAndWithdrawn() {
        PlannerDate date = givenOpenPlan().get(0);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByPlanDateIdAndUserId(DATE_ID, USER_ID)).thenReturn(Optional.empty());
        when(attendanceRepository.saveAndFlush(any(PlannerAttendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlannerPlanDTO answered = service.setAttendance(DATE_ID, "yes", USER_ID);

        assertThat(answered.dates()).hasSize(1);
        assertThat(answered.members()).singleElement()
                .satisfies(member -> assertThat(member.excluded()).isFalse());

        PlannerAttendance stored = new PlannerAttendance(date, user, PlannerAttendanceState.YES);
        stored.setState(PlannerAttendanceState.MAYBE);
        when(attendanceRepository.findByPlanDateIdAndUserId(DATE_ID, USER_ID)).thenReturn(Optional.of(stored));
        when(attendanceRepository.findByPlanDateIdIn(any())).thenReturn(List.of(stored));

        PlannerPlanDTO changed = service.setAttendance(DATE_ID, "maybe", USER_ID);

        assertThat(changed.dates().get(0).attendances()).singleElement()
                .satisfies(attendance -> {
                    assertThat(attendance.userId()).isEqualTo(USER_ID);
                    assertThat(attendance.state()).isEqualTo("MAYBE");
                });

        service.clearAttendance(DATE_ID, USER_ID);

        verify(attendanceRepository).delete(stored);
    }

    // --- the roster -----------------------------------------------------------

    @Test
    @DisplayName("leaving a plan is a row the member can undo")
    void leavingAPlanCanBeUndoneByTheMember() {
        givenOpenPlan();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(exclusionRepository.findByPlanIdAndUserId(PLAN_ID, USER_ID)).thenReturn(Optional.empty());

        service.leavePlan(PLAN_ID, USER_ID);

        ArgumentCaptor<PlannerMemberExclusion> saved = ArgumentCaptor.forClass(PlannerMemberExclusion.class);
        verify(exclusionRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().isRemovedByOwner())
                .as("leaving is the member's own move, so they may come back")
                .isFalse();

        when(exclusionRepository.findByPlanIdAndUserId(PLAN_ID, USER_ID))
                .thenReturn(Optional.of(saved.getValue()));

        service.rejoinPlan(PLAN_ID, USER_ID);

        verify(exclusionRepository).delete(saved.getValue());
    }

    @Test
    @DisplayName("a member removed by the owner cannot put themselves back")
    void anOwnerRemovalCannotBeUndoneByTheMember() {
        givenOpenPlan();
        when(exclusionRepository.findByPlanIdAndUserId(PLAN_ID, USER_ID))
                .thenReturn(Optional.of(new PlannerMemberExclusion(plan, user, "User7", true)));

        assertThatThrownBy(() -> service.rejoinPlan(PLAN_ID, USER_ID))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("only they can put you back");
        verify(exclusionRepository, never()).delete(any(PlannerMemberExclusion.class));
    }

    @Test
    @DisplayName("removing somebody else is the campaign owner's")
    void removingAnotherMemberIsTheCampaignOwners() {
        givenOpenPlan();
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, OTHER_ID)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findById(OTHER_ID)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.removeMember(PLAN_ID, OTHER_ID, USER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);

        when(campaignPermissionService.isCampaignOwner(CAMPAIGN_ID, USER_ID)).thenReturn(true);
        when(exclusionRepository.findByPlanIdAndUserId(PLAN_ID, OTHER_ID)).thenReturn(Optional.empty());

        service.removeMember(PLAN_ID, OTHER_ID, USER_ID);

        ArgumentCaptor<PlannerMemberExclusion> saved = ArgumentCaptor.forClass(PlannerMemberExclusion.class);
        verify(exclusionRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().isRemovedByOwner()).isTrue();
        assertThat(saved.getValue().getUser()).isEqualTo(other);
    }

    @Test
    @DisplayName("putting somebody back is the owner's, and does nothing when they are not out")
    void puttingAnotherMemberBackIsTheCampaignOwners() {
        givenOpenPlan();
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, OTHER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.addMember(PLAN_ID, OTHER_ID, USER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);

        when(campaignPermissionService.isCampaignOwner(CAMPAIGN_ID, USER_ID)).thenReturn(true);

        // Not out, so there is nothing to undo — and nothing to fail on either.
        when(exclusionRepository.findByPlanIdAndUserId(PLAN_ID, OTHER_ID)).thenReturn(Optional.empty());

        service.addMember(PLAN_ID, OTHER_ID, USER_ID);

        verify(exclusionRepository, never()).delete(any(PlannerMemberExclusion.class));

        // The owner's own removal, undone: their row goes, their answers stay.
        PlannerMemberExclusion exclusion = new PlannerMemberExclusion(plan, other, "User7", true);
        when(exclusionRepository.findByPlanIdAndUserId(PLAN_ID, OTHER_ID))
                .thenReturn(Optional.of(exclusion));

        service.addMember(PLAN_ID, OTHER_ID, USER_ID);

        verify(exclusionRepository).delete(exclusion);
        verify(attendanceRepository, never()).delete(any(PlannerAttendance.class));
    }

    @Test
    @DisplayName("the plan list is for participants only")
    void thePlanListIsForParticipantsOnly() {
        when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(campaign));
        givenAddonEnabled();
        when(campaignPermissionService.isParticipant(CAMPAIGN_ID, OTHER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.listPlans(CAMPAIGN_ID, OTHER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
    }
}
