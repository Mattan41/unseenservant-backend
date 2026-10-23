package org.kruskopf.backend.journal.service;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

/**
 * A participant's private journal, scoped to one campaign.
 * <p>
 * Two rules shape everything here:
 * <ul>
 *   <li><strong>Privacy.</strong> A journal is readable and writable only by the
 *       participant who owns it. There is no endpoint that takes a journal id or
 *       another user's id, and a page that belongs to somebody else is answered
 *       exactly like an unknown one (404), so nothing about it leaks — not even
 *       that it exists. A caller who is not a participant of the campaign is
 *       refused with 403.</li>
 *   <li><strong>The last page is virtual.</strong> A client always shows one blank
 *       page after the stored ones ({@code pageCount + 1}) and nothing is stored
 *       for it. Writing there creates a page; clearing a stored last page deletes
 *       it again (and any further blank pages at the end), so a journal never has
 *       more than one trailing blank page.</li>
 * </ul>
 * A read never writes: the journal row appears with the first write, and existing
 * participants need no backfill.
 */
@Service
public class JournalService {

    private final JournalRepository journalRepository;
    private final JournalPageRepository journalPageRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final CampaignPermissionService campaignPermissionService;
    private final JournalMapper journalMapper;
    private final TransactionTemplate transactionTemplate;

    /** Maximum length of one page, in characters (the authoritative limit). */
    private final int maxPageLength;

    /** Maximum number of stored pages in one journal. */
    private final int maxPages;

    public JournalService(JournalRepository journalRepository,
                          JournalPageRepository journalPageRepository,
                          CampaignRepository campaignRepository,
                          UserRepository userRepository,
                          CampaignPermissionService campaignPermissionService,
                          JournalMapper journalMapper,
                          TransactionTemplate transactionTemplate,
                          @Value("${app.journal.max-page-length:10000}") int maxPageLength,
                          @Value("${app.journal.max-pages:500}") int maxPages) {
        this.journalRepository = journalRepository;
        this.journalPageRepository = journalPageRepository;
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.campaignPermissionService = campaignPermissionService;
        this.journalMapper = journalMapper;
        this.transactionTemplate = transactionTemplate;
        this.maxPageLength = maxPageLength;
        this.maxPages = maxPages;
    }

    /**
     * The caller's journal in a campaign. Pure read: it never creates the journal
     * row, and answers an empty journal when nothing has been written yet.
     */
    @Transactional(readOnly = true)
    public JournalDTO getJournal(Long campaignId, long userId) {
        requireCampaign(campaignId);
        requireParticipant(campaignId, userId);

        return journalRepository.findByCampaignIdAndUserId(campaignId, userId)
                .map(journal -> loadJournalDTO(journal.getId()))
                .orElseGet(() -> JournalMapper.emptyJournal(campaignId, userId));
    }

    /**
     * Adds a page, either at the end of the book ({@code afterPageId} null) or
     * after the given page, which shifts every later page's number by one.
     * <p>
     * Appending a blank page is a no-op: the trailing page is always a blank,
     * virtual one, so storing another blank at the end would leave two.
     * <p>
     * Deliberately <strong>not</strong> {@code @Transactional}: when two first
     * writes race, the loser's transaction has to end before the work can be
     * retried, because only a transaction started after the winner's commit can
     * see the winner's journal row (MySQL runs {@code REPEATABLE READ}).
     */
    public JournalDTO createPage(Long campaignId, long userId, Long afterPageId, String content) {
        requireCampaign(campaignId);
        requireParticipant(campaignId, userId);
        requireWithinLimit(content);

        // Nothing to write on this path, so it needs no transaction at all.
        if (afterPageId == null && isBlank(content)) {
            return journalRepository.findByCampaignIdAndUserId(campaignId, userId)
                    .map(journal -> loadJournalDTO(journal.getId()))
                    .orElseGet(() -> JournalMapper.emptyJournal(campaignId, userId));
        }

        try {
            return transactionTemplate.execute(status ->
                    createPageInTransaction(campaignId, userId, afterPageId, content));
        } catch (DataIntegrityViolationException _) {
            // Two first writes raced on the unique key (campaign_id, user_id).
            // MySQL reports the duplicate only once the winner has committed, so
            // this retry runs in a fresh transaction that can see its row.
            return transactionTemplate.execute(status ->
                    createPageInTransaction(campaignId, userId, afterPageId, content));
        }
    }

    private JournalDTO createPageInTransaction(Long campaignId, long userId, Long afterPageId, String content) {
        Journal journal = journalRepository.findByCampaignIdAndUserId(campaignId, userId)
                .orElseGet(() -> createJournal(campaignId, userId));
        Long journalId = journal.getId();

        long pageCount = journalPageRepository.countByJournalId(journalId);
        if (pageCount >= maxPages) {
            throw new InvalidRequestException("A journal may have at most " + maxPages + " pages");
        }

        int targetPosition;
        if (afterPageId == null) {
            Integer maxPosition = journalPageRepository.findMaxPosition(journalId);
            targetPosition = maxPosition == null ? 1 : maxPosition + 1;
        } else {
            JournalPage anchor = loadOwnedPage(afterPageId, userId);
            if (!anchor.getJournal().getId().equals(journalId)) {
                throw pageNotFound(afterPageId);
            }
            targetPosition = anchor.getPosition() + 1;
            journalPageRepository.shiftPositionsUp(journalId, targetPosition);
        }

        JournalPage page = new JournalPage();
        // The bulk shift (if any) cleared the persistence context, so the journal
        // is re-read rather than reused as a detached instance.
        page.setJournal(journalRepository.findById(journalId)
                .orElseThrow(() -> journalNotFound(journalId)));
        page.setPosition(targetPosition);
        page.setContent(content);
        journalPageRepository.saveAndFlush(page);

        return loadJournalDTO(journalId);
    }

    /**
     * Rewrites one page.
     * <p>
     * Clearing the last stored page removes it — and keeps removing blank pages at
     * the end — so the cleared page becomes the virtual trailing page again and
     * the book never has two trailing blanks. A blank page in the middle is kept.
     */
    @Transactional
    public JournalPageSaveDTO updatePage(Long pageId, long userId, String content, Long version) {
        JournalPage page = loadOwnedPage(pageId, userId);
        requireWithinLimit(content);

        if (version == null) {
            // Required. The token is what makes this an optimistic lock: without
            // it the comparison below would be skipped, and Hibernate's @Version
            // does not help because the page is re-read inside this request.
            throw new InvalidRequestException("version is required");
        }

        if (version.longValue() != page.getVersion()) {
            throw new ConcurrentUpdateException(
                    "This journal page was updated by someone else. Reload and try again.");
        }

        Long journalId = page.getJournal().getId();

        if (isBlank(content) && page.getPosition() == lastPosition(journalId)) {
            journalPageRepository.delete(page);
            journalPageRepository.flush();
            collapseTrailingBlankPages(journalId);
            return new JournalPageSaveDTO(null, (int) journalPageRepository.countByJournalId(journalId));
        }

        page.setContent(content);
        JournalPage saved = journalPageRepository.saveAndFlush(page);

        return new JournalPageSaveDTO(
                journalMapper.toPageDTO(saved),
                (int) journalPageRepository.countByJournalId(journalId));
    }

    /** Deletes a page; the pages after it close the gap and keep their order. */
    @Transactional
    public JournalDTO deletePage(Long pageId, long userId) {
        JournalPage page = loadOwnedPage(pageId, userId);
        Long journalId = page.getJournal().getId();
        int position = page.getPosition();

        journalPageRepository.delete(page);
        journalPageRepository.flush();
        journalPageRepository.shiftPositionsDown(journalId, position);

        return loadJournalDTO(journalId);
    }

    /**
     * The journal of a participant, created the first time they write.
     * <p>
     * The unique key {@code (campaign_id, user_id)} is what makes a concurrent
     * first write safe: the loser's insert fails with a
     * {@link DataIntegrityViolationException}, which {@link #createPage} answers
     * with one retry in a new transaction. The failure deliberately propagates
     * from here — a re-read inside the failed transaction could not see the
     * winner's row, because its snapshot predates the winner's commit.
     */
    private Journal createJournal(Long campaignId, long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));

        return journalRepository.saveAndFlush(new Journal(campaign, user));
    }

    /**
     * Removes blank pages from the end of the book until the last stored page has
     * content — the collapse that keeps a journal at one trailing blank page.
     */
    private void collapseTrailingBlankPages(Long journalId) {
        Optional<JournalPage> last = journalPageRepository.findFirstByJournalIdOrderByPositionDesc(journalId);
        while (last.isPresent() && isBlank(last.get().getContent())) {
            journalPageRepository.delete(last.get());
            journalPageRepository.flush();
            last = journalPageRepository.findFirstByJournalIdOrderByPositionDesc(journalId);
        }
    }

    /**
     * Loads a page the caller may edit. A page id that exists but belongs to
     * somebody else answers exactly like an unknown one, so no other
     * participant's writing is discoverable — not even its existence.
     */
    private JournalPage loadOwnedPage(Long pageId, long userId) {
        JournalPage page = journalPageRepository.findById(pageId)
                .orElseThrow(() -> pageNotFound(pageId));

        if (!page.getJournal().getUser().getId().equals(userId)) {
            throw pageNotFound(pageId);
        }

        requireParticipant(page.getJournal().getCampaign().getId(), userId);
        return page;
    }

    private JournalDTO loadJournalDTO(Long journalId) {
        Journal journal = journalRepository.findById(journalId)
                .orElseThrow(() -> journalNotFound(journalId));

        return journalMapper.toJournalDTO(
                journal,
                journalPageRepository.findByJournalIdOrderByPositionAscIdAsc(journalId));
    }

    private int lastPosition(Long journalId) {
        Integer maxPosition = journalPageRepository.findMaxPosition(journalId);
        return maxPosition == null ? 0 : maxPosition;
    }

    private void requireWithinLimit(String content) {
        if (content != null && content.length() > maxPageLength) {
            throw new InvalidRequestException(
                    "A journal page may be at most " + maxPageLength + " characters");
        }
    }

    private void requireCampaign(Long campaignId) {
        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found with id: " + campaignId);
        }
    }

    private void requireParticipant(Long campaignId, long userId) {
        if (!campaignPermissionService.isParticipant(campaignId, userId)) {
            throw new UnauthorizedAccessException("User is not a participant in this campaign");
        }
    }

    private static boolean isBlank(String content) {
        return content == null || content.isBlank();
    }

    private static ResourceNotFoundException journalNotFound(Long journalId) {
        return new ResourceNotFoundException("Journal not found with id: " + journalId);
    }

    private static ResourceNotFoundException pageNotFound(Long pageId) {
        return new ResourceNotFoundException("Journal page not found with id: " + pageId);
    }
}
