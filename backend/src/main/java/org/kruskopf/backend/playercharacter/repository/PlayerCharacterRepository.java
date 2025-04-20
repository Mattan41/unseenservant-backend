package org.kruskopf.backend.playercharacter.repository;

import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.kruskopf.backend.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, Long> {
    List<PlayerCharacter> findByOwner(User owner);

    @EntityGraph(attributePaths = {"owner", "campaign"})
    List<PlayerCharacter> findByCampaignId(Long campaignId);

    @Modifying
    @Query("UPDATE PlayerCharacter pc SET pc.campaign = NULL WHERE pc.owner.id = :userId AND pc.campaign.id = :campaignId")
    void removeCampaignReferenceForUser(@Param("userId") Long userId, @Param("campaignId") Long campaignId);
}