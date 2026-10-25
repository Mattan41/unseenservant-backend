package org.kruskopf.backend.addon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.kruskopf.backend.entity.AuditedEntity;
import org.kruskopf.backend.addon.AddonKey;
import org.kruskopf.backend.campaign.entity.Campaign;

/**
 * One add-on a campaign owner has switched on.
 * <p>
 * The row's existence <em>is</em> the enabled state: there is no boolean to keep
 * in step, and switching the add-on off deletes the row. The unique key on
 * {@code (campaign_id, addon_key)} makes enabling idempotent, and
 * {@link #createdBy} is the owner who enabled it — it is the only audit fact the
 * table needs, since the campaign records the rest.
 * <p>
 * Deliberately generic: nothing about the planner is stored here, so the next
 * add-on needs no migration (see {@code V3__campaign_planner.sql}).
 */
@Entity
@Table(name = "campaign_addon", uniqueConstraints = {
        @UniqueConstraint(name = "uq_campaign_addon_campaign_key", columnNames = {"campaign_id", "addon_key"})
})
public class CampaignAddon extends AuditedEntity {

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "addon_key", nullable = false, length = 50)
    private AddonKey addonKey;

    public CampaignAddon() {
    }

    public CampaignAddon(Campaign campaign, AddonKey addonKey) {
        this.campaign = campaign;
        this.addonKey = addonKey;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public AddonKey getAddonKey() {
        return addonKey;
    }

    public void setAddonKey(AddonKey addonKey) {
        this.addonKey = addonKey;
    }

}
