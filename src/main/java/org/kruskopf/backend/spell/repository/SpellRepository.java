package org.kruskopf.backend.spell.repository;

import org.kruskopf.backend.spell.entity.Spell;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SpellRepository extends JpaRepository<Spell, String> {
    List<Spell> findByNameContainingIgnoreCase(String name);
    Page<Spell> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
