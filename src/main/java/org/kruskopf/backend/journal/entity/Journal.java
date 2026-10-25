package org.kruskopf.backend.journal.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.entity.BaseEntity;
import org.kruskopf.backend.user.entity.User;

import java.util.ArrayList;
import java.util.List;

/**
 * A participant's private journal in one campaign: one book per participant per
 * campaign, held by the unique key on {@code (campaign_id, user_id)}.
 * <p>
 * Only its owner can read or write it — not another participant, not the
 * campaign owner and not a GM. That is enforced in
 * {@link org.kruskopf.backend.journal.service.JournalService} on every call;
 * nothing about another user's journal is exposed through the API.
 * <p>
 * The row is created on the first write, never on a read, so opening a journal
 * stores nothing. It is deliberately keyed on the {@link User} rather than on
 * the {@code campaign_user} entry, so removing a participant keeps their pages
 * and rejoining restores access to them.
 */
@Entity
@Table(name = "journal", uniqueConstraints = {
        @UniqueConstraint(name = "uq_journal_campaign_user", columnNames = {"campaign_id", "user_id"})
})
public class Journal extends BaseEntity {

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "journal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalPage> pages = new ArrayList<>();

    public Journal() {
    }

    public Journal(Campaign campaign, User user) {
        this.campaign = campaign;
        this.user = user;
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

    public List<JournalPage> getPages() {
        return pages;
    }

    public void setPages(List<JournalPage> pages) {
        this.pages = pages;
    }

}
