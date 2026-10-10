package org.kruskopf.backend.playercharacter.entity;

import jakarta.persistence.*;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.dnd5e.entity.Dnd5eCharacterData;
import org.kruskopf.backend.offworlders.entity.OffworldersCharacterData;
import org.kruskopf.backend.user.entity.User;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * System-agnostic core character entity.
 * <p>
 * Holds only fields that are meaningful for any tabletop RPG system. All
 * system-specific data is stored in a dedicated 1:1 entity selected by
 * {@link #systemType} (e.g. {@link Dnd5eCharacterData} for {@link GameSystem#DND5E}
 * or {@link OffworldersCharacterData} for {@link GameSystem#OFFWORLDERS}).
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "game_character", indexes = {@Index(name = "idx_character_owner_id", columnList = "owner_id")})
public class GameCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_type", nullable = false)
    private GameSystem systemType = GameSystem.DND5E;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Public backstory — visible to all campaign members. */
    @Column(columnDefinition = "TEXT")
    private String backstory;

    /** Private backstory — visible to the owner and the campaign GM only. */
    @Column(name = "private_backstory", columnDefinition = "TEXT")
    private String privateBackstory;

    @Column
    private String imageUrl;

    @OneToOne(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Dnd5eCharacterData dnd5eData;

    @OneToOne(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private OffworldersCharacterData offworldersData;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;

    // Constructors
    public GameCharacter() {
    }

    public GameCharacter(User owner, Campaign campaign, String name, GameSystem systemType) {
        this.owner = owner;
        this.campaign = campaign;
        this.name = name;
        this.systemType = systemType != null ? systemType : GameSystem.DND5E;
        this.createdAt = LocalDateTime.now();
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
    public Long getId() {
        return id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }
}
