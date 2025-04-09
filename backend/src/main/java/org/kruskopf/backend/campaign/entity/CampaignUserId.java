package org.kruskopf.backend.campaign.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public record CampaignUserId(
        Long campaignId,
        Long userId
) implements Serializable {
}
