package org.kruskopf.backend.journal.repository;

import org.kruskopf.backend.journal.entity.JournalPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Page access for a journal.
 * <p>
 * The two shifting queries keep {@code position} contiguous, so the stored
 * number stays the page's place in the book. They are bulk updates, which
 * bypass the persistence context, hence
 * {@code clearAutomatically = true}: the stale copies must not be reused
 * afterwards. Positions are packed, so a page inserted after {@code P} makes
 * every later number one higher and a deleted page makes them one lower.
 */
public interface JournalPageRepository extends JpaRepository<JournalPage, Long> {

    /** The whole book, in reading order. */
    List<JournalPage> findByJournalIdOrderByPositionAscIdAsc(Long journalId);

    long countByJournalId(Long journalId);

    @Query("SELECT MAX(p.position) FROM JournalPage p WHERE p.journal.id = :journalId")
    Integer findMaxPosition(@Param("journalId") Long journalId);

    Optional<JournalPage> findFirstByJournalIdOrderByPositionDesc(Long journalId);

    /** Make room at {@code positionFrom}: page P and everything after it moves one step. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JournalPage p SET p.position = p.position + 1 "
            + "WHERE p.journal.id = :journalId AND p.position >= :positionFrom")
    int shiftPositionsUp(@Param("journalId") Long journalId, @Param("positionFrom") int positionFrom);

    /** Close the gap a deleted page left behind: everything after it moves one step back. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE JournalPage p SET p.position = p.position - 1 "
            + "WHERE p.journal.id = :journalId AND p.position > :positionFrom")
    int shiftPositionsDown(@Param("journalId") Long journalId, @Param("positionFrom") int positionFrom);
}
