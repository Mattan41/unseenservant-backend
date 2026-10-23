package org.kruskopf.backend.journal.repository;

import org.kruskopf.backend.journal.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    /** One journal per participant per campaign (enforced by a unique key). */
    Optional<Journal> findByCampaignIdAndUserId(Long campaignId, Long userId);

    boolean existsByCampaignIdAndUserId(Long campaignId, Long userId);
}
