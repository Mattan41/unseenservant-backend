package org.kruskopf.backend.offworlders;

/**
 * Offworlders attribute scores. Stored as a JSON column via
 * {@link OffworldersStatsConverter}.
 * <p>
 * Attributes range from -1 to +3 (0 is the human average). Derived statistics
 * are not persisted here: Health is {@code max(1, 12 + strength + agility)}.
 */
public class OffworldersStats {
    private int strength;
    private int agility;
    private int intelligence;
    private int willpower;

    public OffworldersStats() {
    }

    public OffworldersStats(int strength, int agility, int intelligence, int willpower) {
        this.strength = strength;
        this.agility = agility;
        this.intelligence = intelligence;
        this.willpower = willpower;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getAgility() {
        return agility;
    }

    public void setAgility(int agility) {
        this.agility = agility;
    }

    public int getIntelligence() {
        return intelligence;
    }

    public void setIntelligence(int intelligence) {
        this.intelligence = intelligence;
    }

    public int getWillpower() {
        return willpower;
    }

    public void setWillpower(int willpower) {
        this.willpower = willpower;
    }

    @Override
    public String toString() {
        return "OffworldersStats{" +
                "strength=" + strength +
                ", agility=" + agility +
                ", intelligence=" + intelligence +
                ", willpower=" + willpower +
                '}';
    }
}
