package org.kruskopf.backend.offworlders;

/**
 * A single free-form inventory entry on an Offworlders character (1.6+).
 * <p>
 * There is deliberately no fixed catalog: this is the catch-all for custom
 * weapons, gear, and anything else. Weapons that carry a damage type are
 * modelled separately by {@link OffworldersWeapon}. Stored as JSON in a TEXT
 * column via {@link OffworldersItemsConverter}.
 */
public class OffworldersItem {
    private String name = "";
    private String description = "";

    public OffworldersItem() {
    }

    public OffworldersItem(String name, String description) {
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

    @Override
    public String toString() {
        return "OffworldersItem{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
