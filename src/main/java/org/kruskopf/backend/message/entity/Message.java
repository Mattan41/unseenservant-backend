package org.kruskopf.backend.message.entity;

import jakarta.persistence.*;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.entity.BaseEntity;
import org.kruskopf.backend.user.entity.User;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_message_user_id", columnList = "user_id"),
        @Index(name = "idx_message_created_at", columnList = "createdAt")
})
public class Message extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "message_body", columnDefinition = "LONGTEXT")
    private String messageBody;

    // Constructors
    public Message() {
    }

    public Message(Campaign campaign, User user, String messageBody) {
        this.campaign = campaign;
        this.user = user;
        this.messageBody = messageBody;
    }

    // Getters and Setters

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

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

}
