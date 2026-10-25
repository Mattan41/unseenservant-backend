package org.kruskopf.backend.campaign.entity;

import jakarta.persistence.*;
import org.kruskopf.backend.entity.AuditedEntity;
import org.kruskopf.backend.message.entity.Message;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.kruskopf.backend.user.entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Campaign extends AuditedEntity {

    private String name;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    /**
     * Private description — visible to campaign GMs only (a non-GM owner does
     * not receive it). Surfaced in the UI as "GM notes".
     */
    @Column(name = "private_description", columnDefinition = "LONGTEXT")
    private String privateDescription;

    /**
     * World lore &amp; background — the campaign's public setting material,
     * shown in the "World Lore &amp; Background" section. Always returned,
     * unlike {@link #privateDescription}.
     */
    @Column(name = "world_lore", columnDefinition = "LONGTEXT")
    private String worldLore;

    @Column(length = 1024)
    private String imageUrl;

    /**
     * Optional primary game system for the campaign. Drives which
     * system-specific sub-sections are shown; {@code null} means "not chosen".
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_system")
    private GameSystem primarySystem;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CampaignUser> participants = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    private List<Message> messages = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    public Campaign() {
    }

    public Campaign(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrivateDescription() {
        return privateDescription;
    }

    public void setPrivateDescription(String privateDescription) {
        this.privateDescription = privateDescription;
    }

    public String getWorldLore() {
        return worldLore;
    }

    public void setWorldLore(String worldLore) {
        this.worldLore = worldLore;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public GameSystem getPrimarySystem() {
        return primarySystem;
    }

    public void setPrimarySystem(GameSystem primarySystem) {
        this.primarySystem = primarySystem;
    }

    public List<CampaignUser> getParticipants() {
        return participants;
    }

    public void setParticipants(List<CampaignUser> participants) {
        this.participants = participants;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    @Transient
    public boolean isOwnedBy(Long userId) {
        return owner != null && owner.getId().equals(userId);
    }
}
