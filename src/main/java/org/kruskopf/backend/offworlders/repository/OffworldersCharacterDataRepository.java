package org.kruskopf.backend.offworlders.repository;

import org.kruskopf.backend.offworlders.entity.OffworldersCharacterData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OffworldersCharacterDataRepository extends JpaRepository<OffworldersCharacterData, Long> {
}
