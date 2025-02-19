package org.kruskopf.backend.campaign;

import jakarta.persistence.*;
import org.kruskopf.backend.user.entity.User;

@Entity
@Table(name = "campaign_user")
public class CampaignUser {

    @EmbeddedId
    private CampaignUserId id;

    @ManyToOne
    @MapsId("campaignId")
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private CampaignRole role;

    @Column(nullable = false)
    private String nickname; // campaign nickname

    public CampaignUser() {
    }

    public CampaignUser(Campaign campaign, User user, CampaignRole role, String nickname) {
        this.campaign = campaign;
        this.user = user;
        this.role = role;
        this.nickname = nickname;
        this.id = new CampaignUserId(campaign.getId(), user.getId());
    }


    // Getters and Setters
    public CampaignUserId getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public CampaignRole getRole() {
        return role;
    }

    public void setRole(CampaignRole role) {
        this.role = role;
    }

}

