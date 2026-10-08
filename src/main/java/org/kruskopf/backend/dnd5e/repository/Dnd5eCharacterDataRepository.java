package org.kruskopf.backend.dnd5e.repository;

import org.kruskopf.backend.dnd5e.entity.Dnd5eCharacterData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Dnd5eCharacterDataRepository extends JpaRepository<Dnd5eCharacterData, Long> {
}
