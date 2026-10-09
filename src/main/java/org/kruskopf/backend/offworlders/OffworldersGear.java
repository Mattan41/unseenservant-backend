package org.kruskopf.backend.offworlders;

/**
 * Offworlders gear block. Stored as a JSON column via {@link OffworldersGearConverter}.
 * <p>
 * Kept deliberately small for the character-sheet pass (rules p.6 / p.12): a
 * primary light weapon and an optional secondary weapon, an optional armor type
 * and a free-text catch-all for miscellaneous items. Full weapon/armor
 * automation (damage dice, mitigation, heavy penalties) is out of scope.
 * <p>
 * The numeric armor rating lives on {@link org.kruskopf.backend.offworlders.entity.OffworldersCharacterData#getArmor()}
 * so it stays a single source of truth; {@link #armorType} is the descriptive label.
 */
public class OffworldersGear {
    private String primaryWeapon = "";
    private String primaryWeaponType = "Light";
    private String secondaryWeapon = "";
    private String secondaryWeaponType = "";
    private String armorType = "";
    private String notes = "";

    public OffworldersGear() {
    }

    public String getPrimaryWeapon() {
        return primaryWeapon;
    }

    public void setPrimaryWeapon(String primaryWeapon) {
        this.primaryWeapon = primaryWeapon;
    }

    public String getPrimaryWeaponType() {
        return primaryWeaponType;
    }

    public void setPrimaryWeaponType(String primaryWeaponType) {
        this.primaryWeaponType = primaryWeaponType;
    }

    public String getSecondaryWeapon() {
        return secondaryWeapon;
    }

    public void setSecondaryWeapon(String secondaryWeapon) {
        this.secondaryWeapon = secondaryWeapon;
    }

    public String getSecondaryWeaponType() {
        return secondaryWeaponType;
    }

    public void setSecondaryWeaponType(String secondaryWeaponType) {
        this.secondaryWeaponType = secondaryWeaponType;
    }

    public String getArmorType() {
        return armorType;
    }

    public void setArmorType(String armorType) {
        this.armorType = armorType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "OffworldersGear{" +
                "primaryWeapon='" + primaryWeapon + '\'' +
                ", primaryWeaponType='" + primaryWeaponType + '\'' +
                ", secondaryWeapon='" + secondaryWeapon + '\'' +
                ", secondaryWeaponType='" + secondaryWeaponType + '\'' +
                ", armorType='" + armorType + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
