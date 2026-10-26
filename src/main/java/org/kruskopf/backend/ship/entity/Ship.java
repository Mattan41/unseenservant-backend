package org.kruskopf.backend.ship.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.entity.BaseEntity;
import org.kruskopf.backend.json.StringListConverter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Offworlders starship, associated 1:1 with a {@link Campaign} (unique FK).
 * <p>
 * Only meaningful when the campaign's primary system is {@code OFFWORLDERS}.
 * The defaults mirror the rulebook (p.13): a blank slate with a small cargo
 * hold, {@value #DEFAULT_DAMAGE} damage, {@value #DEFAULT_HULL} Hull,
 * {@value #DEFAULT_ARMOR} Armor and {@value #DEFAULT_MAX_DRIVE_FUEL} Max Drive
 * Fuel (starting fully fueled).
 * <p>
 * {@link #upgrades} is a small, player-editable catalog stored as a JSON array
 * in a TEXT column (same pattern as the Offworlders character skills/abilities).
 * The two repeatable upgrades may appear twice. {@link #version} provides
 * optimistic concurrency: a client must send the version it loaded and a stale
 * write is rejected with HTTP 409.
 */
@Entity
@Table(name = "ship")
public class Ship extends BaseEntity {

    public static final int DEFAULT_HULL = 15;
    public static final int DEFAULT_ARMOR = 0;
    public static final String DEFAULT_DAMAGE = "1D6";
    public static final int DEFAULT_MAX_DRIVE_FUEL = 4;
    public static final int DEFAULT_CREDITS = 0;

    @OneToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "campaign_id", nullable = false, unique = true)
    private Campaign campaign;

    /**
     * Optimistic-concurrency version, owned by Hibernate. Every update carries
     * {@code WHERE version = ?}, so a writer whose row changed since it loaded
     * fails with {@link org.springframework.orm.ObjectOptimisticLockingFailureException}
     * (mapped to HTTP 409) instead of silently overwriting the other change.
     * <p>
     * Never assign this: the service layer compares the client's token against
     * the loaded value, and Hibernate performs the increment on flush.
     */
    @Version
    @Column(nullable = false)
    private long version = 0;

    @Column(nullable = false)
    private String name = "";

    @Column(nullable = false)
    private int hull = DEFAULT_HULL;

    @Column(name = "hull_max", nullable = false)
    private int hullMax = DEFAULT_HULL;

    @Column(nullable = false)
    private int armor = DEFAULT_ARMOR;

    @Column(nullable = false)
    private String damage = DEFAULT_DAMAGE;

    @Column(name = "drive_fuel", nullable = false)
    private int driveFuel = DEFAULT_MAX_DRIVE_FUEL;

    @Column(name = "max_drive_fuel", nullable = false)
    private int maxDriveFuel = DEFAULT_MAX_DRIVE_FUEL;

    /** Shared crew credits balance (minimum 0, no upper cap). */
    @Column(nullable = false)
    private int credits = DEFAULT_CREDITS;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> upgrades = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes = "";

    /** Profile image shown for the ship (defaults to the frontend's defaultShip.svg). */
    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    /** Gallery images (drawings, maps, etc.), stored as a JSON array of URLs. */
    @Column(name = "image_urls", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> imageUrls = new ArrayList<>();

    public Ship() {
    }

    /**
     * A fresh ship with the rulebook defaults (fields are initialised inline).
     */
    public static Ship createDefault() {
        return new Ship();
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public long getVersion() {
        return version;
    }

    /** For tests only. Production code must let Hibernate manage the version. */
    public void setVersion(long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHull() {
        return hull;
    }

    public void setHull(int hull) {
        this.hull = hull;
    }

    public int getHullMax() {
        return hullMax;
    }

    public void setHullMax(int hullMax) {
        this.hullMax = hullMax;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = armor;
    }

    public String getDamage() {
        return damage;
    }

    public void setDamage(String damage) {
        this.damage = damage;
    }

    public int getDriveFuel() {
        return driveFuel;
    }

    public void setDriveFuel(int driveFuel) {
        this.driveFuel = driveFuel;
    }

    public int getMaxDriveFuel() {
        return maxDriveFuel;
    }

    public void setMaxDriveFuel(int maxDriveFuel) {
        this.maxDriveFuel = maxDriveFuel;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public List<String> getUpgrades() {
        return upgrades;
    }

    public void setUpgrades(List<String> upgrades) {
        this.upgrades = upgrades;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

}
