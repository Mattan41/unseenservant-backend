package org.kruskopf.backend.playercharacter.entity;

/**
 * Identifies which tabletop RPG rule system a {@link GameCharacter} belongs to.
 * <p>
 * New systems are registered here. System-specific data lives in a dedicated
 * entity associated 1:1 with the character (e.g. {@code Dnd5eCharacterData}
 * for {@link #DND5E}).
 */
public enum GameSystem {
    DND5E,
    OFFWORLDERS
}
