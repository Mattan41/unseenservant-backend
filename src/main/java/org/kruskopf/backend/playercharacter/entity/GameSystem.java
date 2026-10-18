package org.kruskopf.backend.playercharacter.entity;

/**
 * Identifies which tabletop RPG rule system a {@link GameCharacter} belongs to.
 * <p>
 * New systems are registered here. System-specific data lives in a dedicated
 * entity associated 1:1 with the character (e.g. {@code Dnd5eCharacterData}
 * for {@link #DND5E}).
 * <p>
 * A character may also have <em>no</em> system at all — that is {@code null} and
 * deliberately not a member of this enum (see {@code Campaign#primarySystem} for
 * the same convention). When adding a constant here, extend the
 * {@code chk_character_system_type} CHECK in
 * {@code V17__allow_character_without_system.sql} as well.
 */
public enum GameSystem {
    DND5E,
    OFFWORLDERS
}
