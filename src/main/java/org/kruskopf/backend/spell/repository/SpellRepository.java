package org.kruskopf.backend.spell.repository;

import org.kruskopf.backend.spell.entity.Spell;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpellRepository extends JpaRepository<Spell, String> {
    List<Spell> findByNameContainingIgnoreCase(String name);
}
