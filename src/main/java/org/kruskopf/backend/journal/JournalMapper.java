package org.kruskopf.backend.journal;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.journal.dto.JournalDTO;
import org.kruskopf.backend.journal.dto.JournalPageDTO;
import org.kruskopf.backend.journal.entity.Journal;
import org.kruskopf.backend.journal.entity.JournalPage;
import org.kruskopf.backend.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JournalMapper {

    /**
     * Maps a journal and its stored pages. The order of {@code pages} is the
     * reading order, so the DTO's page numbers are their 1-based ranks.
     */
    public JournalDTO toJournalDTO(Journal journal, List<JournalPage> pages) {
        List<JournalPageDTO> pageDTOs = pages.stream().map(this::toPageDTO).toList();

        Campaign campaign = journal.getCampaign();
        User user = journal.getUser();

        return new JournalDTO(
                journal.getId(),
                campaign != null ? campaign.getId() : null,
                user != null ? user.getId() : null,
                pageDTOs.size(),
                pageDTOs
        );
    }

    public JournalPageDTO toPageDTO(JournalPage page) {
        return new JournalPageDTO(
                page.getId(),
                page.getPosition(),
                page.getContent(),
                page.getVersion(),
                page.getCreatedAt(),
                page.getUpdatedAt()
        );
    }

    /**
     * The journal of a participant who has not written anything yet: no row
     * exists, so it has no id and no pages.
     */
    public static JournalDTO emptyJournal(Long campaignId, long userId) {
        return new JournalDTO(null, campaignId, userId, 0, List.of());
    }
}
