package org.kruskopf.backend.playercharacter.repository;

import org.kruskopf.backend.playercharacter.entity.GameCharacter;
import org.kruskopf.backend.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameCharacterRepository extends JpaRepository<GameCharacter, Long> {
    List<GameCharacter> findByOwner(User owner);

    @EntityGraph(attributePaths = {"owner", "campaign"})
    List<GameCharacter> findByCampaignId(Long campaignId);

    @Modifying
    @Query("UPDATE GameCharacter gc SET gc.campaign = NULL WHERE gc.owner.id = :userId AND gc.campaign.id = :campaignId")
    void removeCampaignReferenceForUser(@Param("userId") Long userId, @Param("campaignId") Long campaignId);
}
