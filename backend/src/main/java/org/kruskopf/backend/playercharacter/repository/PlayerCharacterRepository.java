package org.kruskopf.backend.playercharacter.repository;

import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, Long> {
}