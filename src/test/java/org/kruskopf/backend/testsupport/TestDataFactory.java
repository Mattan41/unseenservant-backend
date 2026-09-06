package org.kruskopf.backend.testsupport;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.kruskopf.backend.spell.entity.Spell;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;

import java.util.HashSet;

/**
 * Shared factory methods for creating fully-populated test entities.
 * <p>
 * All methods return transient (non‑persisted) objects so that callers can
 * decide whether to save them via a repository or attach them to other managed
 * entities before persisting.
 */
public final class TestDataFactory {

    private TestDataFactory() {
        // Utility class
    }

    /**
     * Builds a valid {@link User} with every NOT‑NULL field populated using
     * sensible defaults derived from {@code username}.
     *
     * @param username the username to use; also used to derive email, providerId, etc.
     * @return a transient {@link User} instance
     */
    public static User aUser(String username) {
        User user = new User();
        user.setProviderId("provider-" + username);
        user.setProviderType(ProviderType.GOOGLE);
        user.setEmail(username + "@example.com");
        user.setFullName(username);
        user.setDisplayName(username);
        user.setUserName(username);
        user.setRole(UserRole.USER);
        user.setPassword("password");
        return user;
    }

    /**
     * Convenience overload that calls {@link #aUser(String)} and persists the
     * result through the given repository.
     *
     * @param repo     the {@link UserRepository} to use for saving
     * @param username the username to use
     * @return the persisted {@link User}
     */
    public static User aUser(UserRepository repo, String username) {
        return repo.save(aUser(username));
    }

    /**
     * Builds a valid {@link PlayerCharacter} with all mandatory fields set.
     * The {@code spells} collection is initialised to an empty set and
     * {@code campaign} is left null.
     *
     * @param owner the owning {@link User}
     * @return a transient {@link PlayerCharacter}
     */
    public static PlayerCharacter aCharacter(User owner) {
        PlayerCharacter character = new PlayerCharacter();
        character.setOwner(owner);
        character.setName("Test Hero");
        character.setCharacterClass("Fighter");
        character.setRace("Human");
        character.setSpells(new HashSet<>());
        return character;
    }

    /**
     * Variant that also assigns a campaign.
     *
     * @param owner    the owning {@link User}
     * @param campaign the {@link Campaign} to which the character belongs
     * @return a transient {@link PlayerCharacter} with the given campaign set
     */
    public static PlayerCharacter aCharacter(User owner, Campaign campaign) {
        PlayerCharacter character = aCharacter(owner);
        character.setCampaign(campaign);
        return character;
    }

    /**
     * Builds a transient {@link Spell} with the given identifier, display name
     * and raw JSON payload.
     *
     * @param slug        the slug (primary key)
     * @param name        the human‑readable spell name
     * @param rawJsonData the raw JSON that will be stored in {@code Spell.rawJsonData}
     * @return a transient {@link Spell}
     */
    public static Spell aSpell(String slug, String name, String rawJsonData) {
        return new Spell(slug, name, rawJsonData);
    }
}
