package org.kruskopf.backend.playercharacter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.playercharacter.PlayerCharacterStats;
import org.kruskopf.backend.playercharacter.PlayerCharacterStatsConverter;
import org.kruskopf.backend.user.entity.User;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "game_character", indexes = {@Index(name = "idx_character_owner_id", columnList = "owner_id")})
public class PlayerCharacter {

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

    @Column(nullable = false)
    @Min(value = 1, message = "Level must be at least 1")
    @Max(value = 20, message = "Level must be at most 20")
    private int level = 1;

    @Column(nullable = false)
    private String characterClass;

    @Column(nullable = false)
    private String race;

    @Column
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = PlayerCharacterStatsConverter.class)
    private PlayerCharacterStats characterData;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;

    // Constructors
    public PlayerCharacter() {
    }

    public PlayerCharacter(User owner, Campaign campaign, String name, int level, String characterClass, String race, PlayerCharacterStats characterData) {
        this.owner = owner;
        this.campaign = campaign;
        this.name = name;
        this.level = level;
        this.characterClass = characterClass;
        this.race = race;
        this.characterData = characterData;
        this.createdAt = LocalDateTime.now();
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

    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public PlayerCharacterStats getCharacterData() {
        return characterData;
    }

    public void setCharacterData(PlayerCharacterStats characterData) {
        this.characterData = characterData;
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

