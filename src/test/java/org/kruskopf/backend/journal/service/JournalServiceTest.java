package org.kruskopf.backend.journal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.ConcurrentUpdateException;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.journal.JournalMapper;
import org.kruskopf.backend.journal.dto.JournalDTO;
import org.kruskopf.backend.journal.dto.JournalPageSaveDTO;
import org.kruskopf.backend.journal.entity.Journal;
import org.kruskopf.backend.journal.entity.JournalPage;
import org.kruskopf.backend.journal.repository.JournalPageRepository;
import org.kruskopf.backend.journal.repository.JournalRepository;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rules that live in {@link JournalService}: page positions shifting on
 * insert and delete, the virtual last page (a cleared last page disappears, with
 * any further blank pages at the end), the limits, and the ownership door.
 */
@DisplayName("JournalService")
class JournalServiceTest {

    private static final long CAMPAIGN_ID = 1L;
    private static final long JOURNAL_ID = 4L;
    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 8L;
    private static final int MAX_PAGE_LENGTH = 10_000;
    private static final int MAX_PAGES = 500;

    private JournalRepository journalRepository;
    private JournalPageRepository journalPageRepository;
    private CampaignRepository campaignRepository;
    private UserRepository userRepository;
    private CampaignPermissionService campaignPermissionService;
    private TransactionTemplate transactionTemplate;
    private TransactionStatus transactionStatus;

    private JournalService service;

    private User user;
    private Campaign campaign;
    private Journal journal;

    @BeforeEach
    void setUp() {
        journalRepository = mock(JournalRepository.class);
        journalPageRepository = mock(JournalPageRepository.class);
        campaignRepository = mock(CampaignRepository.class);
        userRepository = mock(UserRepository.class);
        campaignPermissionService = mock(CampaignPermissionService.class);
        // The service runs its write path inside a TransactionTemplate. The unit
        // test has no transaction, so the mock simply runs the callback.
        transactionTemplate = mock(TransactionTemplate.class);
        transactionStatus = mock(TransactionStatus.class);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });

        service = new JournalService(
                journalRepository,
                journalPageRepository,
                campaignRepository,
                userRepository,
                campaignPermissionService,
                new JournalMapper(),
                transactionTemplate,
                MAX_PAGE_LENGTH,
                MAX_PAGES);

        user = userWithId(USER_ID);
        campaign = campaignWithId(CAMPAIGN_ID, user);
        journal = journal(user, campaign);
    }

    // --- fixtures -------------------------------------------------------------

    private static User userWithId(long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Campaign campaignWithId(long id, User owner) {
        Campaign campaign = new Campaign("Test Campaign", "Test Description");
        campaign.setOwner(owner);
        ReflectionTestUtils.setField(campaign, "id", id);
        return campaign;
    }

    private static Journal journal(User owner, Campaign campaign) {
        Journal journal = new Journal(campaign, owner);
        journal.setId(JOURNAL_ID);
        return journal;
    }

    private static JournalPage page(long id, Journal journal, int position, String content) {
        JournalPage page = new JournalPage(journal, position, content);
        page.setId(id);
        return page;
    }

    /** The read the service performs before the ownership rules are exercised. */
    private void givenParticipant() {
        when(campaignRepository.existsById(CAMPAIGN_ID)).thenReturn(true);
        when(campaignPermissionService.isParticipant(eq(CAMPAIGN_ID), eq(USER_ID))).thenReturn(true);
    }

    private void givenExistingJournal(JournalPage... pages) {
        when(journalRepository.findByCampaignIdAndUserId(CAMPAIGN_ID, USER_ID)).thenReturn(Optional.of(journal));
        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(journal));
        when(journalPageRepository.findByJournalIdOrderByPositionAscIdAsc(JOURNAL_ID))
                .thenReturn(List.of(pages));
        when(journalPageRepository.countByJournalId(JOURNAL_ID)).thenReturn((long) pages.length);
    }

    // --- positions ------------------------------------------------------------

    @Test
    @DisplayName("inserting a page after page 1 makes every later page one number higher")
    void insertShiftsLaterPositions() {
        JournalPage first = page(31L, journal, 1, "one");
        JournalPage second = page(32L, journal, 2, "two");
        JournalPage third = page(33L, journal, 3, "three");
        JournalPage fourth = page(34L, journal, 4, "four");

        givenParticipant();
        when(journalRepository.findByCampaignIdAndUserId(CAMPAIGN_ID, USER_ID)).thenReturn(Optional.of(journal));
        when(journalPageRepository.countByJournalId(JOURNAL_ID)).thenReturn(3L);
        when(journalPageRepository.findById(31L)).thenReturn(Optional.of(first));
        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(journal));
        when(journalPageRepository.findByJournalIdOrderByPositionAscIdAsc(JOURNAL_ID))
                .thenReturn(List.of(first, second, third, fourth));

        JournalDTO result = service.createPage(CAMPAIGN_ID, USER_ID, 31L, "inserted");

        ArgumentCaptor<JournalPage> saved = ArgumentCaptor.forClass(JournalPage.class);
        verify(journalPageRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPosition())
                .as("the new page takes page 2, so the old page 2 becomes page 3")
                .isEqualTo(2);
        assertThat(saved.getValue().getContent()).isEqualTo("inserted");
        verify(journalPageRepository).shiftPositionsUp(JOURNAL_ID, 2);
        assertThat(result.pageCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("appending a page puts it after the last one and shifts nothing")
    void appendUsesTheNextPosition() {
        givenParticipant();
        when(journalRepository.findByCampaignIdAndUserId(CAMPAIGN_ID, USER_ID)).thenReturn(Optional.of(journal));
        when(journalPageRepository.countByJournalId(JOURNAL_ID)).thenReturn(2L);
        when(journalPageRepository.findMaxPosition(JOURNAL_ID)).thenReturn(2);
        when(journalRepository.findById(JOURNAL_ID)).thenReturn(Optional.of(journal));
        when(journalPageRepository.findByJournalIdOrderByPositionAscIdAsc(JOURNAL_ID))
                .thenReturn(List.of(page(31L, journal, 1, "one"), page(32L, journal, 2, "two")));

        service.createPage(CAMPAIGN_ID, USER_ID, null, "three");

        ArgumentCaptor<JournalPage> saved = ArgumentCaptor.forClass(JournalPage.class);
        verify(journalPageRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPosition()).isEqualTo(3);
        verify(journalPageRepository, never()).shiftPositionsUp(anyLong(), anyInt());
    }

    @Test
    @DisplayName("deleting a page makes every later page one number lower")
    void deleteShiftsLaterPositionsDown() {
        JournalPage first = page(31L, journal, 1, "one");
        JournalPage second = page(32L, journal, 2, "two");
        JournalPage third = page(33L, journal, 3, "three");

        givenParticipant();
        givenExistingJournal(first, third);
        when(journalPageRepository.findById(32L)).thenReturn(Optional.of(second));

        JournalDTO result = service.deletePage(32L, USER_ID);

        verify(journalPageRepository).delete(second);
        verify(journalPageRepository).shiftPositionsDown(JOURNAL_ID, 2);
        assertThat(result.pageCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("a journal refuses a page past the configured maximum")
    void refusesAPagePastTheMaximum() {
        givenParticipant();
        when(journalRepository.findByCampaignIdAndUserId(CAMPAIGN_ID, USER_ID)).thenReturn(Optional.of(journal));
        when(journalPageRepository.countByJournalId(JOURNAL_ID)).thenReturn((long) MAX_PAGES);

        assertThatThrownBy(() -> service.createPage(CAMPAIGN_ID, USER_ID, null, "one more"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(String.valueOf(MAX_PAGES));
        verify(journalPageRepository, never()).saveAndFlush(any());
    }

    // --- the virtual last page ------------------------------------------------

    @Test
    @DisplayName("writing a blank last page removes it, so it becomes the virtual page again")
    void clearingTheLastPageRemovesIt() {
        JournalPage first = page(31L, journal, 1, "one");
        JournalPage last = page(32L, journal, 2, "two");

        givenParticipant();
        when(journalPageRepository.findById(32L)).thenReturn(Optional.of(last));
        when(journalPageRepository.findMaxPosition(JOURNAL_ID)).thenReturn(2);
        when(journalPageRepository.findFirstByJournalIdOrderByPositionDesc(JOURNAL_ID))
                .thenReturn(Optional.of(first));
        when(journalPageRepository.countByJournalId(JOURNAL_ID)).thenReturn(1L);

        JournalPageSaveDTO result = service.updatePage(32L, USER_ID, "   ", 0L);

        assertThat(result.page()).isNull();
        assertThat(result.pageCount()).isEqualTo(1);
        verify(journalPageRepository).delete(last);
        verify(journalPageRepository, never()).delete(first);
    }

    @Test
    @DisplayName("clearing a last page next to a blank page removes both: one trailing blank")
    void clearingTheLastPageCollapsesFurtherBlankPages() {
        JournalPage first = page(31L, journal, 1, "one");
        JournalPage blank = page(32L, journal, 2, "");
        JournalPage last = page(33L, journal, 3, "three");

        givenParticipant();
        when(journalPageRepository.findById(33L)).thenReturn(Optional.of(last));
        when(journalPageRepository.findMaxPosition(JOURNAL_ID)).thenReturn(3);
        when(journalPageRepository.findFirstByJournalIdOrderByPositionDesc(JOURNAL_ID))
                .thenReturn(Optional.of(blank), Optional.of(first));
        when(journalPageRepository.countByJournalId(JOURNAL_ID)).thenReturn(1L);

        JournalPageSaveDTO result = service.updatePage(33L, USER_ID, "", 0L);

        assertThat(result.page()).isNull();
        assertThat(result.pageCount()).isEqualTo(1);
        verify(journalPageRepository).delete(last);
        verify(journalPageRepository).delete(blank);
        verify(journalPageRepository, never()).delete(first);
    }

    @Test
    @DisplayName("a blank page in the middle is kept")
    void blankMiddlePageIsKept() {
        JournalPage middle = page(32L, journal, 2, "two");

        givenParticipant();
        when(journalPageRepository.findById(32L)).thenReturn(Optional.of(middle));
        when(journalPageRepository.findMaxPosition(JOURNAL_ID)).thenReturn(3);
        when(journalPageRepository.saveAndFlush(any(JournalPage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(journalPageRepository.countByJournalId(JOURNAL_ID)).thenReturn(3L);

        JournalPageSaveDTO result = service.updatePage(32L, USER_ID, "", 0L);

        assertThat(result.page()).isNotNull();
        assertThat(result.page().content()).isEmpty();
        assertThat(result.pageCount()).isEqualTo(3);
        verify(journalPageRepository, never()).delete(any(JournalPage.class));
    }

    @Test
    @DisplayName("appending a blank page is a no-op and creates nothing at all")
    void appendingABlankPageWritesNothing() {
        givenParticipant();
        when(journalRepository.findByCampaignIdAndUserId(CAMPAIGN_ID, USER_ID)).thenReturn(Optional.empty());

        JournalDTO result = service.createPage(CAMPAIGN_ID, USER_ID, null, "  ");

        assertThat(result.id()).isNull();
        assertThat(result.pageCount()).isZero();
        verify(journalRepository, never()).saveAndFlush(any());
        verify(journalPageRepository, never()).saveAndFlush(any());
    }

    // --- limits ---------------------------------------------------------------

    @Test
    @DisplayName("a page longer than the configured limit is refused")
    void refusesAnOverLongPage() {
        givenParticipant();

        assertThatThrownBy(() -> service.createPage(
                CAMPAIGN_ID, USER_ID, null, "a".repeat(MAX_PAGE_LENGTH + 1)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(String.valueOf(MAX_PAGE_LENGTH));
        verify(journalPageRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("writing an over-long page is refused")
    void refusesAnOverLongRewrite() {
        givenParticipant();
        when(journalPageRepository.findById(31L)).thenReturn(Optional.of(page(31L, journal, 1, "one")));

        assertThatThrownBy(() -> service.updatePage(
                31L, USER_ID, "a".repeat(MAX_PAGE_LENGTH + 1), null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining(String.valueOf(MAX_PAGE_LENGTH));
        verify(journalPageRepository, never()).saveAndFlush(any());
    }

    // --- the ownership door ---------------------------------------------------

    @Test
    @DisplayName("a page that belongs to somebody else is answered as not found")
    void foreignPageIsNotFound() {
        Journal otherJournal = journal(userWithId(OTHER_USER_ID), campaign);
        JournalPage foreign = page(32L, otherJournal, 1, "secret");
        when(journalPageRepository.findById(32L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.updatePage(32L, USER_ID, "hijack", 0L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Journal page not found with id: 32");
        assertThatThrownBy(() -> service.deletePage(32L, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Journal page not found with id: 32");
        verify(journalPageRepository, never()).delete(any(JournalPage.class));
    }

    @Test
    @DisplayName("an unknown page id is not found")
    void unknownPageIsNotFound() {
        when(journalPageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletePage(999L, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Journal page not found with id: 999");
    }

    @Test
    @DisplayName("an unknown campaign is not found before the participant check")
    void unknownCampaignIsNotFound() {
        when(campaignRepository.existsById(CAMPAIGN_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getJournal(CAMPAIGN_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Campaign not found with id: " + CAMPAIGN_ID);
    }

    @Test
    @DisplayName("a caller who is not a participant is refused")
    void nonParticipantIsRefused() {
        when(campaignRepository.existsById(CAMPAIGN_ID)).thenReturn(true);
        when(campaignPermissionService.isParticipant(eq(CAMPAIGN_ID), eq(USER_ID))).thenReturn(false);

        assertThatThrownBy(() -> service.getJournal(CAMPAIGN_ID, USER_ID))
                .isInstanceOf(UnauthorizedAccessException.class);
        assertThatThrownBy(() -> service.createPage(CAMPAIGN_ID, USER_ID, null, "text"))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    // --- optimistic locking ---------------------------------------------------

    @Test
    @DisplayName("a stale version token is rejected instead of overwriting the page")
    void staleVersionIsRejected() {
        JournalPage page = page(31L, journal, 1, "one");
        page.setVersion(3);

        givenParticipant();
        when(journalPageRepository.findById(31L)).thenReturn(Optional.of(page));

        assertThatThrownBy(() -> service.updatePage(31L, USER_ID, "mine", 2L))
                .isInstanceOf(ConcurrentUpdateException.class);
        verify(journalPageRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("the version the client loaded is accepted")
    void currentVersionIsAccepted() {
        JournalPage page = page(31L, journal, 1, "one");
        page.setVersion(3);

        givenParticipant();
        when(journalPageRepository.findById(31L)).thenReturn(Optional.of(page));
        when(journalPageRepository.findMaxPosition(JOURNAL_ID)).thenReturn(1);
        when(journalPageRepository.saveAndFlush(any(JournalPage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(journalPageRepository.countByJournalId(JOURNAL_ID)).thenReturn(1L);

        JournalPageSaveDTO result = service.updatePage(31L, USER_ID, "mine", 3L);

        assertThat(result.page()).isNotNull();
        assertThat(result.page().content()).isEqualTo("mine");
        assertThat(result.page().pageNumber()).isEqualTo(1);
        assertThat(result.pageCount()).isEqualTo(1);
    }
}
