package org.kruskopf.backend.offworlders.entity;

import jakarta.persistence.*;
import org.kruskopf.backend.offworlders.OffworldersStats;
import org.kruskopf.backend.offworlders.OffworldersStatsConverter;
import org.kruskopf.backend.offworlders.StringListConverter;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;

import java.util.ArrayList;
import java.util.List;

/**
 * Offworlders-specific character data.
 * <p>
 * Associated 1:1 with {@link GameCharacter} (shared primary key) and only
 * relevant when the owning character's {@link GameCharacter#getSystemType()} is
 * {@code OFFWORLDERS}.
 * <p>
 * Freeform-friendly lists ({@link #skills}, {@link #abilities}) are stored as
 * JSON arrays via {@link StringListConverter} rather than join tables, since the
 * canonical catalogs are small and users may add custom entries.
 */
@Entity
@Table(name = "offworlders_character_data")
public class OffworldersCharacterData {

    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "character_id")
    private GameCharacter character;

    @Column(nullable = false)
    private String characterClass = "";

    @Column(nullable = false)
    private String species = "";

    @Column(nullable = false)
    private String look = "";

    @Column(nullable = false)
    private int xp = 0;

    @Column(nullable = false)
    private int health = 12;

    @Column(nullable = false)
    private int armor = 0;

    @Column(nullable = false)
    private int supply = 0;

    @Column(name = "supply_max", nullable = false)
    private int supplyMax = 0;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = OffworldersStatsConverter.class)
    private OffworldersStats stats = new OffworldersStats();

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> skills = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> abilities = new ArrayList<>();

    public OffworldersCharacterData() {
    }

    public OffworldersCharacterData(String characterClass, String species, OffworldersStats stats) {
        this.characterClass = characterClass;
        this.species = species;
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

    public String getCharacterClass() {
        return characterClass;
    }

    public void setCharacterClass(String characterClass) {
        this.characterClass = characterClass;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getLook() {
        return look;
    }

    public void setLook(String look) {
        this.look = look;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = armor;
    }

    public int getSupply() {
        return supply;
    }

    public void setSupply(int supply) {
        this.supply = supply;
    }

    public int getSupplyMax() {
        return supplyMax;
    }

    public void setSupplyMax(int supplyMax) {
        this.supplyMax = supplyMax;
    }

    public OffworldersStats getStats() {
        return stats;
    }

    public void setStats(OffworldersStats stats) {
        this.stats = stats;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getAbilities() {
        return abilities;
    }

    public void setAbilities(List<String> abilities) {
        this.abilities = abilities;
    }
}
