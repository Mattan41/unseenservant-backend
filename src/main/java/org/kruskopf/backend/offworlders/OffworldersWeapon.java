package org.kruskopf.backend.offworlders;

/**
 * A typed weapon on an Offworlders character (1.6+): a category
 * (Light / Medium / Heavy) plus a free-text description.
 * <p>
 * Damage (1D6 / 1D6+1 / 1D6+2) and the "heavy" flag follow from {@link #type}
 * and are derived on the client, so they are deliberately not stored here.
 * Stored as JSON in a TEXT column via {@link OffworldersWeaponsConverter}.
 */
public class OffworldersWeapon {
    private String type = "Light";
    private String description = "";

    public OffworldersWeapon() {
    }

    public OffworldersWeapon(String type, String description) {
        this.type = type;
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "OffworldersWeapon{" +
                "type='" + type + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
