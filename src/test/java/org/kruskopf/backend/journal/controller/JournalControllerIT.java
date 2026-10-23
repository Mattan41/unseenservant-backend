package org.kruskopf.backend.journal.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.auth.JwtService;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.journal.dto.JournalPageCreateDTO;
import org.kruskopf.backend.journal.dto.JournalPageUpdateDTO;
import org.kruskopf.backend.journal.repository.JournalPageRepository;
import org.kruskopf.backend.journal.repository.JournalRepository;
import org.kruskopf.backend.journal.service.JournalService;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The journal contract over HTTP: who may read and write, how page numbers move
 * on insert and delete, the virtual last page, and the limits.
 * <p>
 * Ownership is the point of the feature, so the refusals are asserted as
 * carefully as the successes: another participant and the campaign owner are
 * denied, a foreign page answers like an unknown one, and an anonymous caller is
 * refused by the filter chain.
 */
@AutoConfigureMockMvc
@Transactional
@DisplayName("JournalController Integration Tests")
class JournalControllerIT extends AbstractIntegrationTest {

    private static final String ISO_LOCAL_DATE_TIME =
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JournalRepository journalRepository;
    private final JournalPageRepository journalPageRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignUserRepository campaignUserRepository;
    private final JournalService journalService;
    private final JwtService jwtService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    JournalControllerIT(MockMvc mockMvc,
                        ObjectMapper objectMapper,
                        JournalRepository journalRepository,
                        JournalPageRepository journalPageRepository,
                        UserRepository userRepository,
                        CampaignRepository campaignRepository,
                        CampaignUserRepository campaignUserRepository,
                        JournalService journalService,
                        JwtService jwtService) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.journalRepository = journalRepository;
        this.journalPageRepository = journalPageRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.campaignUserRepository = campaignUserRepository;
        this.journalService = journalService;
        this.jwtService = jwtService;
    }

    private User owner;
    private User participant;
    private User otherParticipant;
    private User outsider;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        // Clean up in FK-safe order.
        journalPageRepository.deleteAll();
        journalRepository.deleteAll();
        campaignUserRepository.deleteAll();
        campaignRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(TestDataFactory.aUser("journal-owner"));
        participant = userRepository.save(TestDataFactory.aUser("journal-participant"));
        otherParticipant = userRepository.save(TestDataFactory.aUser("journal-other"));
        outsider = userRepository.save(TestDataFactory.aUser("journal-outsider"));

        campaign = campaignRepository.save(TestDataFactory.aCampaign(owner));
        campaignUserRepository.save(new CampaignUser(campaign, owner, CampaignRole.GM, "Owner"));
        campaignUserRepository.save(new CampaignUser(campaign, participant, CampaignRole.PLAYER, "Player"));
        campaignUserRepository.save(new CampaignUser(campaign, otherParticipant, CampaignRole.PLAYER, "Other"));
    }

    private String token(User user) {
        return jwtService.generateToken(user);
    }

    private MvcResult getJournal(User actor) throws Exception {
        return mockMvc.perform(get("/api/journals/campaign/" + campaign.getId())
                        .header("Authorization", "Bearer " + token(actor)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private JsonNode createPage(User actor, Long afterPageId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/journals/campaign/" + campaign.getId() + "/pages")
                        .header("Authorization", "Bearer " + token(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new JournalPageCreateDTO(afterPageId, content))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private MvcResult patchPage(User actor, long pageId, String content, Long version) throws Exception {
        return mockMvc.perform(patch("/api/journals/pages/" + pageId)
                        .header("Authorization", "Bearer " + token(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new JournalPageUpdateDTO(content, version))))
                .andReturn();
    }

    private JsonNode page(JsonNode journal, int index) {
        return journal.get("pages").get(index);
    }

    private long pageId(JsonNode journal, int index) {
        return page(journal, index).get("id").asLong();
    }

    @Nested
    @DisplayName("GET /api/journals/campaign/{campaignId}")
    class GetJournal {

        @Test
        @DisplayName("Answers an empty journal and writes nothing")
        void answersAnEmptyJournal() throws Exception {
            mockMvc.perform(get("/api/journals/campaign/" + campaign.getId())
                            .header("Authorization", "Bearer " + token(participant)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(nullValue()))
                    .andExpect(jsonPath("$.campaignId").value(campaign.getId()))
                    .andExpect(jsonPath("$.userId").value(participant.getId()))
                    .andExpect(jsonPath("$.pageCount").value(0))
                    .andExpect(jsonPath("$.pages").isEmpty());

            assertThat(journalRepository.findByCampaignIdAndUserId(campaign.getId(), participant.getId()))
                    .as("a read never creates the journal row")
                    .isEmpty();
            assertThat(journalPageRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Returns the caller's own pages, in reading order")
        void returnsTheCallersOwnPages() throws Exception {
            createPage(participant, null, "one");
            createPage(participant, null, "two");

            mockMvc.perform(get("/api/journals/campaign/" + campaign.getId())
                            .header("Authorization", "Bearer " + token(participant)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageCount").value(2))
                    .andExpect(jsonPath("$.pages[0].pageNumber").value(1))
                    .andExpect(jsonPath("$.pages[0].content").value("one"))
                    .andExpect(jsonPath("$.pages[0].version").value(0))
                    .andExpect(jsonPath("$.pages[0].createdAt").value(matchesPattern(ISO_LOCAL_DATE_TIME)))
                    .andExpect(jsonPath("$.pages[1].pageNumber").value(2))
                    .andExpect(jsonPath("$.pages[1].content").value("two"));
        }

        @Test
        @DisplayName("A participant never sees another participant's journal")
        void aParticipantSeesOnlyTheirOwnJournal() throws Exception {
            createPage(participant, null, "private notes");

            MvcResult result = getJournal(otherParticipant);
            JsonNode journal =
                    objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

            assertThat(journal.get("pageCount").asInt()).isZero();
            assertThat(journal.get("pages").isEmpty()).isTrue();
            assertThat(journal.get("id").isNull()).isTrue();
        }

        @Test
        @DisplayName("Returns 403 for a non-participant")
        void returns403ForNonParticipant() throws Exception {
            mockMvc.perform(get("/api/journals/campaign/" + campaign.getId())
                            .header("Authorization", "Bearer " + token(outsider)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 404 for an unknown campaign")
        void returns404ForUnknownCampaign() throws Exception {
            mockMvc.perform(get("/api/journals/campaign/99999")
                            .header("Authorization", "Bearer " + token(participant)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Refuses an anonymous caller")
        void refusesAnAnonymousCaller() throws Exception {
            mockMvc.perform(get("/api/journals/campaign/" + campaign.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/journals/campaign/{campaignId}/pages")
    class CreatePage {

        @Test
        @DisplayName("Adds pages and numbers them in order")
        void numbersPagesInOrder() throws Exception {
            createPage(participant, null, "one");

            JsonNode journal = createPage(participant, null, "two");

            assertThat(journal.get("pageCount").asInt()).isEqualTo(2);
            assertThat(page(journal, 0).get("pageNumber").asInt()).isEqualTo(1);
            assertThat(page(journal, 0).get("content").asText()).isEqualTo("one");
            assertThat(page(journal, 1).get("pageNumber").asInt()).isEqualTo(2);
            assertThat(page(journal, 1).get("content").asText()).isEqualTo("two");
            assertThat(page(journal, 0).get("createdAt").asText()).matches(ISO_LOCAL_DATE_TIME);
        }

        @Test
        @DisplayName("Inserting after a page pushes every later page along")
        void insertPushesLaterPagesAlong() throws Exception {
            JsonNode journal = createPage(participant, null, "one");
            journal = createPage(participant, null, "two");
            createPage(participant, null, "three");
            long firstPageId = pageId(journal, 0);

            JournalPageCreateDTO insert = new JournalPageCreateDTO(firstPageId, "inserted");
            MvcResult result = mockMvc.perform(post("/api/journals/campaign/" + campaign.getId() + "/pages")
                            .header("Authorization", "Bearer " + token(participant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(insert)))
                    .andExpect(status().isCreated())
                    .andReturn();
            JsonNode after = objectMapper.readTree(
                    result.getResponse().getContentAsString(StandardCharsets.UTF_8));

            assertThat(after.get("pageCount").asInt()).isEqualTo(4);
            assertThat(page(after, 0).get("content").asText()).isEqualTo("one");
            assertThat(page(after, 1).get("content").asText()).isEqualTo("inserted");
            assertThat(page(after, 2).get("content").asText()).isEqualTo("two");
            assertThat(page(after, 3).get("content").asText()).isEqualTo("three");
            assertThat(page(after, 1).get("pageNumber").asInt()).isEqualTo(2);
            assertThat(page(after, 2).get("pageNumber").asInt()).isEqualTo(3);
        }

        @Test
        @DisplayName("A blank page inserted in the middle is kept")
        void blankMiddlePageIsKept() throws Exception {
            JsonNode journal = createPage(participant, null, "one");
            long firstPageId = pageId(journal, 0);
            createPage(participant, null, "two");

            JsonNode after = createPage(participant, firstPageId, "");

            assertThat(after.get("pageCount").asInt()).isEqualTo(3);
            assertThat(page(after, 1).get("content").asText()).isEmpty();
        }

        @Test
        @DisplayName("Appending a blank page changes nothing and creates no journal")
        void appendingABlankPageChangesNothing() throws Exception {
            JsonNode journal = createPage(participant, null, "   ");

            assertThat(journal.get("id").isNull()).isTrue();
            assertThat(journal.get("pageCount").asInt()).isZero();
            assertThat(journalRepository.findByCampaignIdAndUserId(campaign.getId(), participant.getId()))
                    .as("the trailing blank page is virtual: nothing is stored for it")
                    .isEmpty();
        }

        @Test
        @DisplayName("Returns 400 for a page past the character limit")
        void returns400PastTheCharacterLimit() throws Exception {
            mockMvc.perform(post("/api/journals/campaign/" + campaign.getId() + "/pages")
                            .header("Authorization", "Bearer " + token(participant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JournalPageCreateDTO(null, "a".repeat(10001)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(matchesPattern(".*10000.*")));
        }

        @Test
        @DisplayName("Returns 403 for a non-participant")
        void returns403ForNonParticipant() throws Exception {
            mockMvc.perform(post("/api/journals/campaign/" + campaign.getId() + "/pages")
                            .header("Authorization", "Bearer " + token(outsider))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JournalPageCreateDTO(null, "not mine"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 404 for an unknown campaign")
        void returns404ForUnknownCampaign() throws Exception {
            mockMvc.perform(post("/api/journals/campaign/99999/pages")
                            .header("Authorization", "Bearer " + token(participant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JournalPageCreateDTO(null, "text"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/journals/pages/{pageId}")
    class UpdatePage {

        @Test
        @DisplayName("Rewrites a page and answers the page with the new page count")
        void rewritesAPage() throws Exception {
            JsonNode journal = createPage(participant, null, "one");

            mockMvc.perform(patch("/api/journals/pages/" + pageId(journal, 0))
                            .header("Authorization", "Bearer " + token(participant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JournalPageUpdateDTO("Rewritten", 0L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.content").value("Rewritten"))
                    .andExpect(jsonPath("$.page.pageNumber").value(1))
                    .andExpect(jsonPath("$.page.version").value(1))
                    .andExpect(jsonPath("$.pageCount").value(1));
        }

        @Test
        @DisplayName("Clearing the last page removes it and makes it virtual again")
        void clearingTheLastPageRemovesIt() throws Exception {
            JsonNode journal = createPage(participant, null, "one");
            journal = createPage(participant, null, "two");
            long lastPageId = pageId(journal, 1);

            mockMvc.perform(patch("/api/journals/pages/" + lastPageId)
                            .header("Authorization", "Bearer " + token(participant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JournalPageUpdateDTO("  ", 0L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(nullValue()))
                    .andExpect(jsonPath("$.pageCount").value(1));

            assertThat(journalPageRepository.findById(lastPageId))
                    .as("a cleared last page is removed, so no empty row is left behind")
                    .isEmpty();
        }

        @Test
        @DisplayName("Clearing a last page next to a blank page collapses both")
        void clearingTheLastPageCollapsesBlankPages() throws Exception {
            JsonNode journal = createPage(participant, null, "one");
            long firstPageId = pageId(journal, 0);
            createPage(participant, firstPageId, ""); // a blank page in the middle
            journal = createPage(participant, null, "three");
            long lastPageId = pageId(journal, 2);

            mockMvc.perform(patch("/api/journals/pages/" + lastPageId)
                            .header("Authorization", "Bearer " + token(participant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JournalPageUpdateDTO("", 0L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(nullValue()))
                    .andExpect(jsonPath("$.pageCount").value(1));

            mockMvc.perform(get("/api/journals/campaign/" + campaign.getId())
                            .header("Authorization", "Bearer " + token(participant)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageCount").value(1))
                    .andExpect(jsonPath("$.pages[0].content").value("one"));
        }

        @Test
        @DisplayName("A blank page in the middle is kept as an empty page")
        void blankMiddlePageIsKept() throws Exception {
            JsonNode journal = createPage(participant, null, "one");
            journal = createPage(participant, null, "two");
            journal = createPage(participant, null, "three");
            long middlePageId = pageId(journal, 1);

            mockMvc.perform(patch("/api/journals/pages/" + middlePageId)
                            .header("Authorization", "Bearer " + token(participant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JournalPageUpdateDTO("", 0L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.content").value(""))
                    .andExpect(jsonPath("$.pageCount").value(3));
        }

        @Test
        @DisplayName("Returns 400 for a page past the character limit")
        void returns400PastTheCharacterLimit() throws Exception {
            JsonNode journal = createPage(participant, null, "one");

            mockMvc.perform(patch("/api/journals/pages/" + pageId(journal, 0))
                            .header("Authorization", "Bearer " + token(participant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JournalPageUpdateDTO("a".repeat(10001), 0L))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(matchesPattern(".*10000.*")));
        }

        @Test
        @DisplayName("Returns 409 for a stale version instead of overwriting")
        void returns409ForAStaleVersion() throws Exception {
            JsonNode journal = createPage(participant, null, "one");
            long firstPageId = pageId(journal, 0);

            mockMvc.perform(patch("/api/journals/pages/" + firstPageId)
                            .header("Authorization", "Bearer " + token(participant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JournalPageUpdateDTO("someone else was here", 0L))))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/api/journals/pages/" + firstPageId)
                            .header("Authorization", "Bearer " + token(participant))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JournalPageUpdateDTO("mine", 0L))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("The campaign owner gets 404 on another participant's page")
        void campaignOwnerGets404OnAForeignPage() throws Exception {
            JsonNode journal = createPage(participant, null, "private notes");
            long foreignPageId = pageId(journal, 0);

            mockMvc.perform(patch("/api/journals/pages/" + foreignPageId)
                            .header("Authorization", "Bearer " + token(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new JournalPageUpdateDTO("peeking at the GM", 0L))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail")
                            .value("Journal page not found with id: " + foreignPageId));
        }

        @Test
        @DisplayName("Another participant gets 404, worded like an unknown page")
        void anotherParticipantGets404LikeAnUnknownPage() throws Exception {
            JsonNode journal = createPage(participant, null, "private notes");
            long foreignPageId = pageId(journal, 0);

            MvcResult foreign = patchPage(otherParticipant, foreignPageId, "peeking", 0L);
            MvcResult unknown = patchPage(otherParticipant, 99999L, "peeking", 0L);

            JsonNode foreignBody = objectMapper.readTree(
                    foreign.getResponse().getContentAsString(StandardCharsets.UTF_8));
            JsonNode unknownBody = objectMapper.readTree(
                    unknown.getResponse().getContentAsString(StandardCharsets.UTF_8));

            assertThat(foreign.getResponse().getStatus()).isEqualTo(404);
            assertThat(unknown.getResponse().getStatus()).isEqualTo(404);
            assertThat(foreignBody.get("detail").asText())
                    .as("the wording is the unknown-page one, so the page's existence is not revealed")
                    .isEqualTo("Journal page not found with id: " + foreignPageId);
            assertThat(unknownBody.get("detail").asText())
                    .isEqualTo("Journal page not found with id: 99999");
        }
    }

    @Nested
    @DisplayName("DELETE /api/journals/pages/{pageId}")
    class DeletePage {

        @Test
        @DisplayName("Deletes a page and closes the gap in the numbering")
        void deletesAPageAndClosesTheGap() throws Exception {
            JsonNode journal = createPage(participant, null, "one");
            journal = createPage(participant, null, "two");
            journal = createPage(participant, null, "three");
            long middlePageId = pageId(journal, 1);

            mockMvc.perform(delete("/api/journals/pages/" + middlePageId)
                            .header("Authorization", "Bearer " + token(participant)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageCount").value(2))
                    .andExpect(jsonPath("$.pages[0].pageNumber").value(1))
                    .andExpect(jsonPath("$.pages[0].content").value("one"))
                    .andExpect(jsonPath("$.pages[1].pageNumber").value(2))
                    .andExpect(jsonPath("$.pages[1].content").value("three"));

            assertThat(journalPageRepository.findById(middlePageId)).isEmpty();
        }

        @Test
        @DisplayName("Returns 404 for another participant's page and leaves it alone")
        void returns404ForAForeignPage() throws Exception {
            JsonNode journal = createPage(participant, null, "private notes");
            long foreignPageId = pageId(journal, 0);

            mockMvc.perform(delete("/api/journals/pages/" + foreignPageId)
                            .header("Authorization", "Bearer " + token(owner)))
                    .andExpect(status().isNotFound());

            assertThat(journalPageRepository.findById(foreignPageId))
                    .as("a refused delete must not touch the page")
                    .isPresent();
        }

        @Test
        @DisplayName("Returns 404 for an unknown page")
        void returns404ForAnUnknownPage() throws Exception {
            mockMvc.perform(delete("/api/journals/pages/99999")
                            .header("Authorization", "Bearer " + token(participant)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Two concurrent first writes leave exactly one journal row")
    void concurrentFirstWritesLeaveOneJournalRow() throws Exception {
        // Deliberately run outside the test transaction: two writers need two real
        // transactions, and a MockMvc call inside this class would join the test's
        // one. The fixture is committed, and removed again at the end.
        User racer = userRepository.save(TestDataFactory.aUser("journal-race-" + UUID.randomUUID()));
        Campaign raceCampaign = campaignRepository.save(TestDataFactory.aCampaign(racer));
        campaignUserRepository.save(new CampaignUser(raceCampaign, racer, CampaignRole.PLAYER, "Racer"));
        Long campaignId = raceCampaign.getId();
        Long userId = racer.getId();

        try {
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            List<Future<Long>> results = new ArrayList<>();
            try {
                for (int index = 0; index < 2; index++) {
                    final int pageNumber = index;
                    results.add(pool.submit(() -> {
                        start.await();
                        return journalService.createPage(campaignId, userId, null, "page " + pageNumber).id();
                    }));
                }
                start.countDown();

                List<Long> journalIds = new ArrayList<>();
                for (Future<Long> result : results) {
                    journalIds.add(result.get(20, TimeUnit.SECONDS));
                }

                assertThat(journalIds).doesNotContainNull();
                assertThat(journalIds.get(0))
                        .as("both writers must land on the same journal, not on two rows")
                        .isEqualTo(journalIds.get(1));
            } finally {
                pool.shutdownNow();
            }

            assertThat(journalRepository.findByCampaignIdAndUserId(campaignId, userId))
                    .as("the unique key (campaign_id, user_id) must have left exactly one row")
                    .isPresent();
            assertThat(journalRepository.findAll().stream()
                    .filter(journal -> journal.getCampaign().getId().equals(campaignId))
                    .count())
                    .isEqualTo(1L);
        } finally {
            removeCommittedFixture(campaignId, userId);
            // The @BeforeEach fixture was committed too, for the same reason.
            campaignRepository.deleteById(campaign.getId());
            userRepository.deleteAll(List.of(owner, participant, otherParticipant, outsider));
        }
    }

    /** Removes rows this test committed, since no transaction is rolled back. */
    private void removeCommittedFixture(Long campaignId, Long userId) {
        journalPageRepository.deleteAll(journalPageRepository.findAll().stream()
                .filter(page -> page.getJournal().getCampaign().getId().equals(campaignId))
                .toList());
        journalRepository.findByCampaignIdAndUserId(campaignId, userId).ifPresent(journalRepository::delete);
        campaignUserRepository.deleteAll(campaignUserRepository.findByCampaignId(campaignId));
        campaignRepository.deleteById(campaignId);
        userRepository.deleteById(userId);
    }
}
