package org.kruskopf.backend.offworlders;

/**
 * A single free-form inventory entry on an Offworlders character.
 * <p>
 * There is deliberately no fixed item catalog: the player writes the name and
 * the damage expression. {@link #kind} only drives how the sheet groups rows
 * (weapons / armor / generic items). Stored as JSON in a TEXT column via
 * {@link OffworldersItemsConverter}.
 */
public class OffworldersItem {
    private String name = "";
    private String kind = "item";
    private String damage = "";
    private int armorRating = 0;
    private boolean heavy = false;
    private String notes = "";

    public OffworldersItem() {
    }

    public OffworldersItem(String name, String kind, String damage, int armorRating, boolean heavy, String notes) {
        this.name = name;
        this.kind = kind;
        this.damage = damage;
        this.armorRating = armorRating;
        this.heavy = heavy;
        this.notes = notes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getDamage() {
        return damage;
    }

    public void setDamage(String damage) {
        this.damage = damage;
    }

    public int getArmorRating() {
        return armorRating;
    }

    public void setArmorRating(int armorRating) {
        this.armorRating = armorRating;
    }

    public boolean isHeavy() {
        return heavy;
    }

    public void setHeavy(boolean heavy) {
        this.heavy = heavy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "OffworldersItem{" +
                "name='" + name + '\'' +
                ", kind='" + kind + '\'' +
                ", damage='" + damage + '\'' +
                ", armorRating=" + armorRating +
                ", heavy=" + heavy +
                ", notes='" + notes + '\'' +
                '}';
    }
}
