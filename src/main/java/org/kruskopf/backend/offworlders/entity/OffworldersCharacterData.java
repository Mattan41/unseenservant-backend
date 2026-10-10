package org.kruskopf.backend.offworlders.entity;

import jakarta.persistence.*;
import org.kruskopf.backend.offworlders.OffworldersEntry;
import org.kruskopf.backend.offworlders.OffworldersItem;
import org.kruskopf.backend.offworlders.OffworldersItemsConverter;
import org.kruskopf.backend.offworlders.OffworldersStats;
import org.kruskopf.backend.offworlders.OffworldersStatsConverter;
import org.kruskopf.backend.offworlders.OffworldersWeapon;
import org.kruskopf.backend.offworlders.OffworldersWeaponsConverter;
import org.kruskopf.backend.offworlders.OffworldersEntriesConverter;
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
 * Vitals: {@link #health} is the maximum (attributes + {@link #healthModifier})
 * and {@link #armor} the effective rating of the worn armor items, while
 * {@link #currentHealth} is the running total the player tracks (it may exceed
 * {@link #health} to represent temporary HP).
 * <p>
 * Freeform-friendly lists ({@link #skills}, {@link #abilities}) are stored as
 * JSON arrays via {@link OffworldersEntriesConverter} rather than join tables, since the
 * canonical catalogs are small and users may add custom entries. The typed
 * {@link #weapons} and free-text {@link #items} lists follow the same pattern.
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

    @Column(name = "current_health", nullable = false)
    private int currentHealth = 12;

    @Column(name = "health_modifier", nullable = false)
    private int healthModifier = 0;

    @Column(nullable = false)
    private int supply = 3;

    @Column(name = "supply_max", nullable = false)
    private int supplyMax = 3;

    @Column(nullable = false)
    private int credits = 3;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = OffworldersStatsConverter.class)
    private OffworldersStats stats = new OffworldersStats();

    @Column(columnDefinition = "TEXT")
    @Convert(converter = OffworldersEntriesConverter.class)
    private List<OffworldersEntry> skills = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    @Convert(converter = OffworldersEntriesConverter.class)
    private List<OffworldersEntry> abilities = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    @Convert(converter = OffworldersWeaponsConverter.class)
    private List<OffworldersWeapon> weapons = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    @Convert(converter = OffworldersItemsConverter.class)
    private List<OffworldersItem> items = new ArrayList<>();

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

    public int getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = currentHealth;
    }

    public int getHealthModifier() {
        return healthModifier;
    }

    public void setHealthModifier(int healthModifier) {
        this.healthModifier = healthModifier;
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

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public OffworldersStats getStats() {
        return stats;
    }

    public void setStats(OffworldersStats stats) {
        this.stats = stats;
    }

    public List<OffworldersEntry> getSkills() {
        return skills;
    }

    public void setSkills(List<OffworldersEntry> skills) {
        this.skills = skills;
    }

    public List<OffworldersEntry> getAbilities() {
        return abilities;
    }

    public void setAbilities(List<OffworldersEntry> abilities) {
        this.abilities = abilities;
    }

    public List<OffworldersWeapon> getWeapons() {
        return weapons;
    }

    public void setWeapons(List<OffworldersWeapon> weapons) {
        this.weapons = weapons;
    }

    public List<OffworldersItem> getItems() {
        return items;
    }

    public void setItems(List<OffworldersItem> items) {
        this.items = items;
    }
}
