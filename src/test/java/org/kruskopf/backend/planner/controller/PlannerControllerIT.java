package org.kruskopf.backend.planner.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.addon.repository.CampaignAddonRepository;
import org.kruskopf.backend.auth.JwtService;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.planner.dto.PlannerAttendanceUpdateDTO;
import org.kruskopf.backend.planner.dto.PlannerDateCreateDTO;
import org.kruskopf.backend.planner.dto.PlannerDateUpdateDTO;
import org.kruskopf.backend.planner.dto.PlannerPlanCreateDTO;
import org.kruskopf.backend.planner.dto.PlannerPlanUpdateDTO;
import org.kruskopf.backend.planner.dto.PlannerStatusUpdateDTO;
import org.kruskopf.backend.planner.repository.PlannerAttendanceRepository;
import org.kruskopf.backend.planner.repository.PlannerDateRepository;
import org.kruskopf.backend.planner.repository.PlannerMemberExclusionRepository;
import org.kruskopf.backend.planner.repository.PlannerPlanRepository;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The planner contract over HTTP, including the add-on switch that opens it: who
 * may turn the planner on, who may read and change a plan once it is on, how an
 * answer is stored and changed, and what a decided plan refuses.
 * <p>
 * The refusals are asserted as carefully as the successes — an outsider, a
 * non-owner trying to enable the add-on, a member removed by the owner trying to
 * walk back in — because that is where this feature's rules actually live.
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("PlannerController Integration Tests")
class PlannerControllerIT extends AbstractIntegrationTest {

    private static final LocalDate SATURDAY = LocalDate.of(2026, 3, 14);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 3, 15);

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignUserRepository campaignUserRepository;
    private final CampaignAddonRepository campaignAddonRepository;
    private final PlannerPlanRepository plannerPlanRepository;
    private final PlannerDateRepository plannerDateRepository;
    private final PlannerAttendanceRepository plannerAttendanceRepository;
    private final PlannerMemberExclusionRepository plannerMemberExclusionRepository;
    private final JwtService jwtService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    PlannerControllerIT(MockMvc mockMvc,
                        ObjectMapper objectMapper,
                        UserRepository userRepository,
                        CampaignRepository campaignRepository,
                        CampaignUserRepository campaignUserRepository,
                        CampaignAddonRepository campaignAddonRepository,
                        PlannerPlanRepository plannerPlanRepository,
                        PlannerDateRepository plannerDateRepository,
                        PlannerAttendanceRepository plannerAttendanceRepository,
                        PlannerMemberExclusionRepository plannerMemberExclusionRepository,
                        JwtService jwtService) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.campaignUserRepository = campaignUserRepository;
        this.campaignAddonRepository = campaignAddonRepository;
        this.plannerPlanRepository = plannerPlanRepository;
        this.plannerDateRepository = plannerDateRepository;
        this.plannerAttendanceRepository = plannerAttendanceRepository;
        this.plannerMemberExclusionRepository = plannerMemberExclusionRepository;
        this.jwtService = jwtService;
    }

    private User owner;
    private User participant;
    private User otherParticipant;
    private User outsider;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        // Clean up in FK-safe order: answers and dates before the plan they belong
        // to, the add-on switch and the participants before the campaign.
        plannerAttendanceRepository.deleteAll();
        plannerDateRepository.deleteAll();
        plannerMemberExclusionRepository.deleteAll();
        plannerPlanRepository.deleteAll();
        campaignAddonRepository.deleteAll();
        campaignUserRepository.deleteAll();
        campaignRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(TestDataFactory.aUser("planner-owner"));
        participant = userRepository.save(TestDataFactory.aUser("planner-participant"));
        otherParticipant = userRepository.save(TestDataFactory.aUser("planner-other"));
        outsider = userRepository.save(TestDataFactory.aUser("planner-outsider"));

        campaign = campaignRepository.save(TestDataFactory.aCampaign(owner));
        campaignUserRepository.save(new CampaignUser(campaign, owner, CampaignRole.GM, "Owner"));
        campaignUserRepository.save(new CampaignUser(campaign, participant, CampaignRole.PLAYER, "Player"));
        campaignUserRepository.save(new CampaignUser(campaign, otherParticipant, CampaignRole.PLAYER, "Other"));
    }

    // --- helpers --------------------------------------------------------------

    private String token(User user) {
        return jwtService.generateToken(user);
    }

    /** Turns the planner on for the campaign, as its owner. */
    private void enablePlanner() throws Exception {
        mockMvc.perform(post("/api/campaigns/" + campaign.getId() + "/addons/planner")
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk());
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private MvcResult createPlan(User actor, String title) throws Exception {
        return mockMvc.perform(post("/api/planner/campaign/" + campaign.getId() + "/plans")
                        .header("Authorization", "Bearer " + token(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlannerPlanCreateDTO(title, null))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MvcResult addDate(User actor, long planId, LocalDate day) throws Exception {
        return mockMvc.perform(post("/api/planner/plans/" + planId + "/dates")
                        .header("Authorization", "Bearer " + token(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlannerDateCreateDTO(
                                day, LocalTime.of(18, 0), LocalTime.of(22, 0), "Bring dice"))))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MvcResult answer(User actor, long dateId, String state) throws Exception {
        return mockMvc.perform(put("/api/planner/dates/" + dateId + "/attendance")
                        .header("Authorization", "Bearer " + token(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlannerAttendanceUpdateDTO(state))))
                .andExpect(status().isOk())
                .andReturn();
    }

    // --- the add-on switch ----------------------------------------------------

    @Test
    @DisplayName("the owner enables the planner and every campaign answer carries it")
    void theOwnerEnablesThePlanner() throws Exception {
        enablePlanner();

        mockMvc.perform(get("/api/campaigns/" + campaign.getId())
                        .header("Authorization", "Bearer " + token(participant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addons[0]").value("planner"));

        mockMvc.perform(get("/api/campaigns/" + campaign.getId() + "/addons")
                        .header("Authorization", "Bearer " + token(participant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("planner"));
    }

    @Test
    @DisplayName("a campaign without add-ons answers an empty list, not null")
    void aCampaignWithoutAddonsAnswersAnEmptyList() throws Exception {
        mockMvc.perform(get("/api/campaigns/" + campaign.getId())
                        .header("Authorization", "Bearer " + token(participant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addons").isEmpty());
    }

    @Test
    @DisplayName("a participant who is not the owner cannot change the add-ons")
    void aNonOwnerCannotChangeTheAddons() throws Exception {
        mockMvc.perform(post("/api/campaigns/" + campaign.getId() + "/addons/planner")
                        .header("Authorization", "Bearer " + token(participant)))
                .andExpect(status().isForbidden());

        assertThat(campaignAddonRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("an unknown add-on key is refused")
    void anUnknownAddonKeyIsRefused() throws Exception {
        mockMvc.perform(post("/api/campaigns/" + campaign.getId() + "/addons/teleporter")
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("switching the planner off closes it, and switching it back on returns the plans")
    void switchingThePlannerOffAndOnAgainKeepsThePlans() throws Exception {
        enablePlanner();
        long planId = body(createPlan(participant, "Session 12")).get("id").asLong();

        mockMvc.perform(delete("/api/campaigns/" + campaign.getId() + "/addons/planner")
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addons").isEmpty());

        mockMvc.perform(get("/api/planner/campaign/" + campaign.getId() + "/plans")
                        .header("Authorization", "Bearer " + token(participant)))
                .andExpect(status().isBadRequest());

        enablePlanner();

        mockMvc.perform(get("/api/planner/plans/" + planId)
                        .header("Authorization", "Bearer " + token(participant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Session 12"));
    }

    @Test
    @DisplayName("an anonymous caller is refused by the filter chain")
    void anAnonymousCallerIsRefused() throws Exception {
        mockMvc.perform(get("/api/planner/campaign/" + campaign.getId() + "/plans"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/campaigns/" + campaign.getId() + "/addons/planner"))
                .andExpect(status().isUnauthorized());
    }

    // --- plans, dates and answers ---------------------------------------------

    @Test
    @DisplayName("an outsider cannot read or start a plan")
    void anOutsiderIsRefused() throws Exception {
        enablePlanner();

        mockMvc.perform(get("/api/planner/campaign/" + campaign.getId() + "/plans")
                        .header("Authorization", "Bearer " + token(outsider)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/planner/campaign/" + campaign.getId() + "/plans")
                        .header("Authorization", "Bearer " + token(outsider))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlannerPlanCreateDTO("Mine", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("any participant starts a plan, and the whole roster is in it already")
    void anyParticipantStartsAPlanWithTheWholeRoster() throws Exception {
        enablePlanner();

        JsonNode plan = body(createPlan(participant, "  When do we play next?  "));

        assertThat(plan.get("title").asString()).isEqualTo("When do we play next?");
        assertThat(plan.get("status").asString()).isEqualTo("OPEN");
        assertThat(plan.get("createdByUserId").asLong()).isEqualTo(participant.getId());
        assertThat(plan.get("members")).hasSize(3);
        assertThat(plan.get("members").get(0).get("excluded").asBoolean()).isFalse();
        assertThat(plan.get("dates")).isEmpty();
    }

    @Test
    @DisplayName("a date is offered by any participant, and one day cannot be offered twice")
    void aDayCannotBeOfferedTwice() throws Exception {
        enablePlanner();
        long planId = body(createPlan(participant, "Session 12")).get("id").asLong();

        JsonNode afterFirst = body(addDate(otherParticipant, planId, SATURDAY));

        assertThat(afterFirst.get("dates")).hasSize(1);
        assertThat(afterFirst.get("dates").get(0).get("date").asString()).isEqualTo("2026-03-14");
        assertThat(afterFirst.get("dates").get(0).get("note").asString()).isEqualTo("Bring dice");

        mockMvc.perform(post("/api/planner/plans/" + planId + "/dates")
                        .header("Authorization", "Bearer " + token(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlannerDateCreateDTO(
                                SATURDAY, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.field").value("date"));
    }

    @Test
    @DisplayName("answers are per member and per date, and one member changes theirs in place")
    void anAnswerIsOnePerMemberAndDate() throws Exception {
        enablePlanner();
        long planId = body(createPlan(participant, "Session 12")).get("id").asLong();
        long saturdayId = body(addDate(participant, planId, SATURDAY)).get("dates").get(0).get("id").asLong();
        long sundayId = body(addDate(otherParticipant, planId, SUNDAY)).get("dates").get(1).get("id").asLong();

        answer(participant, saturdayId, "YES");
        JsonNode both = body(answer(otherParticipant, saturdayId, "NO"));

        assertThat(both.get("dates").get(0).get("attendances")).hasSize(2);
        assertThat(both.get("dates").get(1).get("attendances")).isEmpty();

        JsonNode changed = body(answer(participant, saturdayId, "MAYBE"));

        assertThat(changed.get("dates").get(0).get("attendances")).hasSize(2);
        assertThat(changed.get("dates").get(0).get("attendances").get(0).get("userId").asLong())
                .isEqualTo(participant.getId());
        assertThat(changed.get("dates").get(0).get("attendances").get(0).get("state").asString())
                .isEqualTo("MAYBE");

        JsonNode withdrawn = body(mockMvc.perform(delete("/api/planner/dates/" + saturdayId + "/attendance")
                        .header("Authorization", "Bearer " + token(participant)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(withdrawn.get("dates").get(0).get("attendances")).hasSize(1);
        assertThat(withdrawn.get("dates").get(1).get("id").asLong()).isEqualTo(sundayId);
    }

    @Test
    @DisplayName("a stale plan version is refused with 409, the loaded one is accepted")
    void aStalePlanVersionIsRefused() throws Exception {
        enablePlanner();
        long planId = body(createPlan(participant, "Session 12")).get("id").asLong();

        mockMvc.perform(patch("/api/planner/plans/" + planId)
                        .header("Authorization", "Bearer " + token(otherParticipant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PlannerPlanUpdateDTO("Renamed", null, 7L))))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/planner/plans/" + planId)
                        .header("Authorization", "Bearer " + token(otherParticipant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PlannerPlanUpdateDTO("Renamed", "For the spring arc", 0L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("a date's day can be corrected, and the times have to make sense")
    void aDatesDayCanBeCorrected() throws Exception {
        enablePlanner();
        long planId = body(createPlan(participant, "Session 12")).get("id").asLong();
        long dateId = body(addDate(participant, planId, SATURDAY)).get("dates").get(0).get("id").asLong();

        mockMvc.perform(patch("/api/planner/dates/" + dateId)
                        .header("Authorization", "Bearer " + token(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlannerDateUpdateDTO(
                                SUNDAY, LocalTime.of(18, 0), null, "Moved to Sunday", 0L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dates[0].date").value("2026-03-15"));

        mockMvc.perform(patch("/api/planner/dates/" + dateId)
                        .header("Authorization", "Bearer " + token(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlannerDateUpdateDTO(
                                SUNDAY, null, LocalTime.of(22, 0), null, 1L))))
                .andExpect(status().isBadRequest());
    }

    // --- deciding, reopening and the roster -----------------------------------

    @Test
    @DisplayName("deciding takes one of the plan's own dates, and freezes the plan until it is reopened")
    void decidingFreezesThePlanUntilItIsReopened() throws Exception {
        enablePlanner();
        long planId = body(createPlan(participant, "Session 12")).get("id").asLong();
        long saturdayId = body(addDate(participant, planId, SATURDAY)).get("dates").get(0).get("id").asLong();

        mockMvc.perform(patch("/api/planner/plans/" + planId + "/status")
                        .header("Authorization", "Bearer " + token(otherParticipant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PlannerStatusUpdateDTO("DECIDED", 999L, 0L))))
                .andExpect(status().isBadRequest());

        JsonNode decided = body(mockMvc.perform(patch("/api/planner/plans/" + planId + "/status")
                        .header("Authorization", "Bearer " + token(otherParticipant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PlannerStatusUpdateDTO("DECIDED", saturdayId, 0L))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(decided.get("status").asString()).isEqualTo("DECIDED");
        assertThat(decided.get("decidedDateId").asLong()).isEqualTo(saturdayId);

        // Frozen: no new dates, no new answers, until somebody reopens it.
        mockMvc.perform(post("/api/planner/plans/" + planId + "/dates")
                        .header("Authorization", "Bearer " + token(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlannerDateCreateDTO(
                                SUNDAY, null, null, null))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/planner/dates/" + saturdayId + "/attendance")
                        .header("Authorization", "Bearer " + token(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlannerAttendanceUpdateDTO("YES"))))
                .andExpect(status().isBadRequest());

        JsonNode reopened = body(mockMvc.perform(patch("/api/planner/plans/" + planId + "/status")
                        .header("Authorization", "Bearer " + token(participant))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PlannerStatusUpdateDTO("OPEN", null, 1L))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(reopened.get("status").asString()).isEqualTo("OPEN");
        assertThat(reopened.get("decidedDateId").isNull()).isTrue();

        answer(participant, saturdayId, "YES");
    }

    private JsonNode memberRow(JsonNode plan, User member) {
        for (JsonNode row : plan.get("members")) {
            if (row.get("userId").asLong() == member.getId()) {
                return row;
            }
        }
        throw new AssertionError("No roster row for " + member.getUserName());
    }

    @Test
    @DisplayName("a member can leave and come back; their answers wait for them")
    void aMemberCanLeaveAndComeBack() throws Exception {
        enablePlanner();
        long planId = body(createPlan(participant, "Session 12")).get("id").asLong();
        long saturdayId = body(addDate(participant, planId, SATURDAY)).get("dates").get(0).get("id").asLong();
        answer(otherParticipant, saturdayId, "YES");

        JsonNode someoneLeft = body(mockMvc.perform(delete("/api/planner/plans/" + planId + "/members/me")
                        .header("Authorization", "Bearer " + token(otherParticipant)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(memberRow(someoneLeft, otherParticipant).get("excluded").asBoolean()).isTrue();
        assertThat(memberRow(someoneLeft, otherParticipant).get("removedByOwner").asBoolean())
                .as("they left, so it is theirs to undo")
                .isFalse();
        assertThat(memberRow(someoneLeft, participant).get("excluded").asBoolean()).isFalse();
        assertThat(someoneLeft.get("dates").get(0).get("attendances"))
                .as("the answer is kept while they are out, so rejoining restores it")
                .hasSize(1);

        JsonNode rejoined = body(mockMvc.perform(put("/api/planner/plans/" + planId + "/members/me")
                        .header("Authorization", "Bearer " + token(otherParticipant)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(memberRow(rejoined, otherParticipant).get("excluded").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("a participant cannot remove somebody else, and the owner's removal is not theirs to undo")
    void removingSomebodyElseIsTheOwners() throws Exception {
        enablePlanner();
        long planId = body(createPlan(participant, "Session 12")).get("id").asLong();

        mockMvc.perform(delete("/api/planner/plans/" + planId + "/members/" + otherParticipant.getId())
                        .header("Authorization", "Bearer " + token(participant)))
                .andExpect(status().isForbidden());

        JsonNode afterRemoval = body(mockMvc.perform(
                        delete("/api/planner/plans/" + planId + "/members/" + otherParticipant.getId())
                                .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(memberRow(afterRemoval, otherParticipant).get("removedByOwner").asBoolean()).isTrue();

        mockMvc.perform(put("/api/planner/plans/" + planId + "/members/me")
                        .header("Authorization", "Bearer " + token(otherParticipant)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("the owner puts a removed member back, and a participant cannot")
    void puttingAMemberBackIsTheOwners() throws Exception {
        enablePlanner();
        long planId = body(createPlan(participant, "Session 12")).get("id").asLong();
        long saturdayId = body(addDate(participant, planId, SATURDAY)).get("dates").get(0).get("id").asLong();
        answer(otherParticipant, saturdayId, "YES");

        mockMvc.perform(delete("/api/planner/plans/" + planId + "/members/" + otherParticipant.getId())
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/planner/plans/" + planId + "/members/" + otherParticipant.getId())
                        .header("Authorization", "Bearer " + token(participant)))
                .andExpect(status().isForbidden());

        JsonNode back = body(mockMvc.perform(
                        put("/api/planner/plans/" + planId + "/members/" + otherParticipant.getId())
                                .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(memberRow(back, otherParticipant).get("excluded").asBoolean()).isFalse();
        assertThat(memberRow(back, otherParticipant).get("removedByOwner").asBoolean()).isFalse();
        assertThat(back.get("dates").get(0).get("attendances"))
                .as("the answer they gave before the removal was never touched")
                .hasSize(1);

        // Putting back somebody who is already in changes nothing, rather than failing.
        JsonNode again = body(mockMvc.perform(
                        put("/api/planner/plans/" + planId + "/members/" + otherParticipant.getId())
                                .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(memberRow(again, otherParticipant).get("excluded").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("deleting a plan is for its creator or the campaign owner, and takes its dates along")
    void deletingAPlanIsForItsCreatorOrTheOwner() throws Exception {
        enablePlanner();
        long planId = body(createPlan(participant, "Session 12")).get("id").asLong();
        long saturdayId = body(addDate(participant, planId, SATURDAY)).get("dates").get(0).get("id").asLong();
        answer(participant, saturdayId, "YES");

        mockMvc.perform(delete("/api/planner/plans/" + planId)
                        .header("Authorization", "Bearer " + token(otherParticipant)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/planner/plans/" + planId)
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isNoContent());

        assertThat(plannerPlanRepository.findAll()).isEmpty();
        assertThat(plannerDateRepository.findAll())
                .as("deleting a plan takes its dates with it")
                .isEmpty();
        assertThat(plannerAttendanceRepository.findAll())
                .as("and their answers")
                .isEmpty();
    }
}
