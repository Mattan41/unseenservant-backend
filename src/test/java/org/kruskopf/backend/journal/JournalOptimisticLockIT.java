package org.kruskopf.backend.journal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.exception.ConcurrentUpdateException;
import org.kruskopf.backend.journal.dto.JournalDTO;
import org.kruskopf.backend.journal.dto.JournalPageDTO;
import org.kruskopf.backend.journal.dto.JournalPageSaveDTO;
import org.kruskopf.backend.journal.entity.JournalPage;
import org.kruskopf.backend.journal.repository.JournalPageRepository;
import org.kruskopf.backend.journal.repository.JournalRepository;
import org.kruskopf.backend.journal.service.JournalService;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves that journal page edits are protected against lost updates at the
 * database level, not only by the version token the client sends.
 * <p>
 * Deliberately <strong>not</strong> {@code @Transactional}: the protection lives
 * in the {@code UPDATE ... WHERE version = ?} that Hibernate issues on flush, and
 * a test-managed transaction that never commits can never observe a conflict.
 * <p>
 * The MySQL container is reused between runs
 * ({@code TestContainersConfiguration} sets {@code withReuse}), so every fixture
 * gets a unique name and is removed again in {@link #tearDown()}.
 */
@DisplayName("Journal page optimistic locking")
class JournalOptimisticLockIT extends AbstractIntegrationTest {

    private final JournalService journalService;
    private final JournalRepository journalRepository;
    private final JournalPageRepository journalPageRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignUserRepository campaignUserRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    JournalOptimisticLockIT(JournalService journalService,
                            JournalRepository journalRepository,
                            JournalPageRepository journalPageRepository,
                            CampaignRepository campaignRepository,
                            CampaignUserRepository campaignUserRepository,
                            UserRepository userRepository,
                            JdbcTemplate jdbcTemplate,
                            TransactionTemplate transactionTemplate) {
        this.journalService = journalService;
        this.journalRepository = journalRepository;
        this.journalPageRepository = journalPageRepository;
        this.campaignRepository = campaignRepository;
        this.campaignUserRepository = campaignUserRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    private User user;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        user = userRepository.save(TestDataFactory.aUser("journal-lock-" + UUID.randomUUID()));
        campaign = campaignRepository.save(TestDataFactory.aCampaign(user));
        campaignUserRepository.save(new CampaignUser(campaign, user, CampaignRole.PLAYER, "Author"));
    }

    @AfterEach
    void tearDown() {
        if (campaign != null && campaign.getId() != null) {
            journalRepository.findByCampaignIdAndUserId(campaign.getId(), user.getId())
                    .ifPresent(journal -> {
                        journalPageRepository.deleteAll(
                                journalPageRepository.findByJournalIdOrderByPositionAscIdAsc(journal.getId()));
                        journalRepository.delete(journal);
                    });
            campaignUserRepository.deleteAll(campaignUserRepository.findByCampaignId(campaign.getId()));
            campaignRepository.deleteById(campaign.getId());
        }
        if (user != null && user.getId() != null) {
            userRepository.deleteById(user.getId());
        }
    }

    @Test
    @DisplayName("a normal save increments the version in the database")
    void saveIncrementsVersion() {
        JournalPageDTO page = firstPage("The first entry");
        assertThat(page.version()).isZero();

        JournalPageSaveDTO saved = journalService.updatePage(
                page.id(), user.getId(), "Rewritten", page.version());

        assertThat(saved.page()).isNotNull();
        assertThat(saved.page().content()).isEqualTo("Rewritten");
        assertThat(saved.page().version()).isEqualTo(1L);
        assertThat(journalPageRepository.findById(page.id()).orElseThrow().getVersion())
                .as("the new version must be persisted, not just returned")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("a stale client version is rejected before any work is done")
    void staleClientVersionIsRejected() {
        JournalPageDTO page = firstPage("The first entry");
        // Captured once: the lambda below has to hold a single invocation, and a
        // later read of the same page does not change the token that was loaded.
        Long pageId = page.id();
        long userId = user.getId();
        Long version = page.version();
        journalService.updatePage(pageId, userId, "First rewrite", version);

        assertThatThrownBy(() -> journalService.updatePage(pageId, userId, "Second rewrite", version))
                .isInstanceOf(ConcurrentUpdateException.class);

        assertThat(journalPageRepository.findById(pageId).orElseThrow().getContent())
                .as("the rejected write must not have landed")
                .isEqualTo("First rewrite");
    }

    @Test
    @DisplayName("a flush against a row that changed underneath is rejected by the version column")
    void staleFlushIsRejectedByTheVersionColumn() {
        Long pageId = firstPage("The first entry").id();

        assertThatThrownBy(() -> bumpVersionAndFlushStaleCopy(pageId))
                .as("Hibernate must reject the stale flush with the exception the "
                        + "controller advice maps to 409")
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(journalPageRepository.findById(pageId).orElseThrow().getContent())
                .as("the stale write must not have landed")
                .isEqualTo("The first entry");
    }

    /**
     * Inside one transaction: dirty the page, then let another connection bump its
     * version before the flush. That is the race the service-level token check
     * cannot see, because it runs before the flush.
     */
    private void bumpVersionAndFlushStaleCopy(Long pageId) {
        transactionTemplate.executeWithoutResult(status -> {
            JournalPage managed = journalPageRepository.findById(pageId).orElseThrow();
            managed.setContent("competing rewrite"); // dirty, so Hibernate issues the UPDATE

            jdbcTemplate.update("UPDATE journal_page SET version = version + 1 WHERE id = ?", pageId);

            // UPDATE journal_page SET ... , version = 1 WHERE id = ? AND version = 0 -> 0 rows.
            journalPageRepository.saveAndFlush(managed);
        });
    }

    /** The first page of the fixture journal, as the client would load it. */
    private JournalPageDTO firstPage(String content) {
        JournalDTO journal = journalService.createPage(campaign.getId(), user.getId(), null, content);
        return journal.pages().getFirst();
    }
}
