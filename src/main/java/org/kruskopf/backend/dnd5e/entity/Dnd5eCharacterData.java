package org.kruskopf.backend.dnd5e.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.kruskopf.backend.dnd5e.Dnd5eCharacterStats;
import org.kruskopf.backend.dnd5e.Dnd5eCharacterStatsConverter;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;
import org.kruskopf.backend.spell.entity.Spell;

import java.util.HashSet;
import java.util.Set;

/**
 * D&D 5e-specific character data.
 * <p>
 * Associated 1:1 with {@link GameCharacter} (shared primary key) and only
 * relevant when the owning character's
 * {@link GameCharacter#getSystemType()} is {@code DND5E}.
 */
@Entity
@Table(name = "dnd5e_character_data")
public class Dnd5eCharacterData {

    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "character_id")
    private GameCharacter character;

    @Column(nullable = false)
    @Min(value = 1, message = "Level must be at least 1")
    @Max(value = 20, message = "Level must be at most 20")
    private int level = 1;

    @Column(nullable = false)
    private String characterClass = "";

    @Column(nullable = false)
    private String race = "";

    @Column(nullable = false)
    private int hitPoints = 0;

    @Column(nullable = false)
    private int armorClass = 10;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = Dnd5eCharacterStatsConverter.class)
    private Dnd5eCharacterStats stats;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "character_spell",
            joinColumns = @JoinColumn(name = "character_id"),
            inverseJoinColumns = @JoinColumn(name = "spell_slug")
    )
    private Set<Spell> spells = new HashSet<>();

    public Dnd5eCharacterData() {
    }

    public Dnd5eCharacterData(int level, String characterClass, String race, Dnd5eCharacterStats stats) {
        this.level = level;
        this.characterClass = characterClass;
        this.race = race;
        this.stats = stats;
    }

    public Long getId() {
        return id;
    }

    public GameCharacter getCharacter() {
        return character;
    }

    public void setCharacter(GameCharacter character) {
        this.character = character;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getCharacterClass() {
        return characterClass;
    }

    public void setCharacterClass(String characterClass) {
        this.characterClass = characterClass;
    }

    public String getRace() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public int getHitPoints() {
        return hitPoints;
    }

    public void setHitPoints(int hitPoints) {
        this.hitPoints = hitPoints;
    }

    public int getArmorClass() {
        return armorClass;
    }

    public void setArmorClass(int armorClass) {
        this.armorClass = armorClass;
    }

    public Dnd5eCharacterStats getStats() {
        return stats;
    }

    public void setStats(Dnd5eCharacterStats stats) {
        this.stats = stats;
    }

    public Set<Spell> getSpells() {
        return spells;
    }

    public void setSpells(Set<Spell> spells) {
        this.spells = spells;
    }
}
