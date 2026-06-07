package org.kruskopf.backend.spell.repository;

import org.kruskopf.backend.spell.entity.Spell;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpellRepository extends JpaRepository<Spell, String> {
}
