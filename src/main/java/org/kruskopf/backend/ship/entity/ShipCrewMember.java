package org.kruskopf.backend.ship.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.kruskopf.backend.entity.BaseEntity;
import org.kruskopf.backend.ship.ShipCrewCharacter;
import org.kruskopf.backend.ship.ShipCrewCharactersConverter;
import org.kruskopf.backend.ship.ShipStringListConverter;
import org.kruskopf.backend.user.entity.User;

import java.util.ArrayList;
import java.util.List;

/**
 * A participant's entry on the ship's crew manifest: free-text notes, gallery
 * images (shown in the ship's single gallery) and the characters they have
 * aboard (name + role on the ship).
 * <p>
 * Keyed on the {@link User}, not the {@code campaign_user} participant row, so
 * the entry survives a participant leaving the campaign (their participant row
 * is deleted) and re-activates unchanged when they rejoin. Whether an entry is
 * "active" is derived from campaign membership, never stored — so there is no
 * flag to keep in sync.
 */
@Entity
@Table(name = "ship_crew", uniqueConstraints = @UniqueConstraint(
        name = "uq_ship_crew_ship_user", columnNames = {"ship_id", "user_id"}))
public class ShipCrewMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "ship_id", nullable = false)
    private Ship ship;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String notes = "";

    /** Gallery image URLs, shown in the ship's single gallery. */
    @Column(name = "image_urls", columnDefinition = "TEXT")
    @Convert(converter = ShipStringListConverter.class)
    private List<String> imageUrls = new ArrayList<>();

    /** Characters aboard: name + optional role on the ship (free text). */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = ShipCrewCharactersConverter.class)
    private List<ShipCrewCharacter> characters = new ArrayList<>();

    public ShipCrewMember() {
    }

    public ShipCrewMember(Ship ship, User user) {
        this.ship = ship;
        this.user = user;
    }

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<ShipCrewCharacter> getCharacters() {
        return characters;
    }

    public void setCharacters(List<ShipCrewCharacter> characters) {
        this.characters = characters;
    }

}
