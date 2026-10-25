package org.kruskopf.backend.playercharacter.entity;

import jakarta.persistence.*;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.dnd5e.entity.Dnd5eCharacterData;
import org.kruskopf.backend.entity.AuditedEntity;
import org.kruskopf.backend.offworlders.entity.OffworldersCharacterData;
import org.kruskopf.backend.user.entity.User;

import java.time.LocalDateTime;

/**
 * System-agnostic core character entity.
 * <p>
 * Holds only fields that are meaningful for any tabletop RPG system. All
 * system-specific data is stored in a dedicated 1:1 entity selected by
 * {@link #systemType} (e.g. {@link Dnd5eCharacterData} for {@link GameSystem#DND5E}
 * or {@link OffworldersCharacterData} for {@link GameSystem#OFFWORLDERS}); when
 * {@link #systemType} is {@code null} the character has no system and no data
 * block is attached.
 */
@Entity
@Table(name = "game_character", indexes = {@Index(name = "idx_character_owner_id", columnList = "owner_id")})
public class GameCharacter extends AuditedEntity {

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(nullable = false)
    private String name;

    /**
     * Ruleset this character is played in, or {@code null} when it deliberately
     * has none: a character may be created without a system, and then only the
     * generic core fields exist — neither data block is attached.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "system_type")
    private GameSystem systemType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Public backstory — visible to all campaign members. */
    @Column(columnDefinition = "TEXT")
    private String backstory;

    /** Private backstory — visible to the owner and the campaign GM only. */
    @Column(name = "private_backstory", columnDefinition = "TEXT")
    private String privateBackstory;

    /** Appearance — generic, meaningful for every system (formerly Offworlders-only "look"). */
    @Column(columnDefinition = "TEXT")
    private String appearance;

    @Column
    private String imageUrl;

    @OneToOne(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Dnd5eCharacterData dnd5eData;

    @OneToOne(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private OffworldersCharacterData offworldersData;

    // Constructors
    public GameCharacter() {
    }

    public GameCharacter(User owner, Campaign campaign, String name, GameSystem systemType) {
        this.owner = owner;
        this.campaign = campaign;
        this.name = name;
        // No system is a valid choice, so it is stored as-is instead of being
        // defaulted to a ruleset the player never picked.
        this.systemType = systemType;
        // Stamped here as well as by the audit listener, so a character has its
        // creation time the moment it is built, before anything is persisted.
        setCreatedAt(LocalDateTime.now());
    }

    /**
     * Attaches the 1:1 D&D 5e data and keeps both sides of the relationship in sync.
     */
    public void attachDnd5eData(Dnd5eCharacterData data) {
        this.dnd5eData = data;
        if (data != null) {
            data.setCharacter(this);
        }
    }

    /**
     * Attaches the 1:1 Offworlders data and keeps both sides of the relationship in sync.
     */
    public void attachOffworldersData(OffworldersCharacterData data) {
        this.offworldersData = data;
        if (data != null) {
            data.setCharacter(this);
        }
    }

    // Getters and Setters

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GameSystem getSystemType() {
        return systemType;
    }

    public void setSystemType(GameSystem systemType) {
        this.systemType = systemType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getBackstory() {
        return backstory;
    }

    public void setBackstory(String backstory) {
        this.backstory = backstory;
    }

    public String getPrivateBackstory() {
        return privateBackstory;
    }

    public void setPrivateBackstory(String privateBackstory) {
        this.privateBackstory = privateBackstory;
    }

    public String getAppearance() {
        return appearance;
    }

    public void setAppearance(String appearance) {
        this.appearance = appearance;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Dnd5eCharacterData getDnd5eData() {
        return dnd5eData;
    }

    public void setDnd5eData(Dnd5eCharacterData dnd5eData) {
        this.dnd5eData = dnd5eData;
    }

    public OffworldersCharacterData getOffworldersData() {
        return offworldersData;
    }

    public void setOffworldersData(OffworldersCharacterData offworldersData) {
        this.offworldersData = offworldersData;
    }

}
