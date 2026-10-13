package org.kruskopf.backend.ship;

/**
 * One character a crew member has aboard the ship: the character's name plus the
 * role they fill on the ship. Both are free text and the role is optional.
 * <p>
 * Deliberately plain text rather than a foreign key to the character, so the
 * entry survives the character being unassigned from the campaign when a
 * participant leaves. Stored as a JSON array via
 * {@link ShipCrewCharactersConverter}.
 */
public class ShipCrewCharacter {

    private String name = "";
    private String roleOnShip = "";

    public ShipCrewCharacter() {
    }

    public ShipCrewCharacter(String name, String roleOnShip) {
        this.name = name;
        this.roleOnShip = roleOnShip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoleOnShip() {
        return roleOnShip;
    }

    public void setRoleOnShip(String roleOnShip) {
        this.roleOnShip = roleOnShip;
    }

    @Override
    public String toString() {
        return "ShipCrewCharacter{" +
                "name='" + name + '\'' +
                ", roleOnShip='" + roleOnShip + '\'' +
                '}';
    }
}
