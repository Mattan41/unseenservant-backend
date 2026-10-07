package org.kruskopf.backend.testsupport;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.message.entity.Message;
import org.kruskopf.backend.dnd5e.entity.Dnd5eCharacterData;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
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
     * Builds a valid {@link Campaign} with all mandatory fields set.
     *
     * @param owner the owning {@link User}
     * @return a transient {@link Campaign}
     */
    public static Campaign aCampaign(User owner) {
        Campaign campaign = new Campaign("Test Campaign", "Test Description");
        campaign.setOwner(owner);
        return campaign;
    }

    /**
     * Builds a valid {@link Message} with all mandatory fields set.
     *
     * @param campaign   the {@link Campaign} the message belongs to
     * @param user       the {@link User} who sent the message
     * @param messageBody the message content
     * @return a transient {@link Message}
     */
    public static Message aMessage(Campaign campaign, User user, String messageBody) {
        return new Message(campaign, user, messageBody);
    }

    /**
     * Builds a valid D&D 5e {@link GameCharacter} with all mandatory fields set.
     * An empty {@link Dnd5eCharacterData} block (with an empty {@code spells}
     * set) is attached and {@code campaign} is left null.
     *
     * @param owner the owning {@link User}
     * @return a transient {@link GameCharacter}
     */
    public static GameCharacter aCharacter(User owner) {
        GameCharacter character = new GameCharacter();
        character.setOwner(owner);
        character.setName("Test Hero");
        character.setSystemType(GameSystem.DND5E);
        Dnd5eCharacterData data = new Dnd5eCharacterData();
        data.setCharacterClass("Fighter");
        data.setRace("Human");
        data.setSpells(new HashSet<>());
        character.attachDnd5eData(data);
        return character;
    }

    /**
     * Variant that also assigns a campaign.
     *
     * @param owner    the owning {@link User}
     * @param campaign the {@link Campaign} to which the character belongs
     * @return a transient {@link GameCharacter} with the given campaign set
     */
    public static GameCharacter aCharacter(User owner, Campaign campaign) {
        GameCharacter character = aCharacter(owner);
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
