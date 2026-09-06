package org.kruskopf.backend.spell.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.AbstractIntegrationTest;
import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.kruskopf.backend.playercharacter.repository.PlayerCharacterRepository;
import org.kruskopf.backend.spell.dto.CharacterSpellResponseDTO;
import org.kruskopf.backend.spell.dto.SpellSaveInputDTO;
import org.kruskopf.backend.spell.entity.Spell;
import org.kruskopf.backend.spell.repository.SpellRepository;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("SpellService Integration Tests")
@Transactional
class SpellServiceIT extends AbstractIntegrationTest {

    private final SpellRepository spellRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final UserRepository userRepository;
    private final SpellService spellService;

    @Autowired
    public SpellServiceIT(SpellRepository spellRepository,
                          PlayerCharacterRepository playerCharacterRepository,
                          UserRepository userRepository,
                          SpellService spellService) {
        this.spellRepository = spellRepository;
        this.playerCharacterRepository = playerCharacterRepository;
        this.userRepository = userRepository;
        this.spellService = spellService;
    }

    @BeforeEach
    void cleanUp() {
        playerCharacterRepository.deleteAll();
        spellRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String username) {
        User user = new User();
        user.setUserName(username);
        user.setEmail(username + "@example.com");
        user.setPassword("password");
        user.setFullName("Test User");
        user.setDisplayName(username);
        user.setProviderId("provider-" + username);
        user.setProviderType(ProviderType.GOOGLE);
        return userRepository.save(user);
    }

    private PlayerCharacter createCharacter(User owner) {
        PlayerCharacter character = new PlayerCharacter();
        character.setOwner(owner);
        character.setName("Test Hero");
        character.setCharacterClass("Fighter");
        character.setRace("Human");
        character.setSpells(new HashSet<>());
        return playerCharacterRepository.save(character);
    }

    private Spell createSpell(String slug, String name, String rawJsonData) {
        Spell spell = new Spell(slug, name, rawJsonData);
        return spellRepository.save(spell);
    }

    @Nested
    @DisplayName("findByNameContainingIgnoreCase")
    class FindByNameContainingIgnoreCaseTests {

        @Test
        void returnsMatchingSpellCaseInsensitive() {
            createSpell("fireball", "Fireball", "{\"name\":\"Fireball\"}");
            createSpell("lightning-bolt", "Lightning Bolt", "{\"name\":\"Lightning Bolt\"}");

            Map<String, Object> result = spellService.searchSpells("fire");

            assertThat(result.get("count")).isEqualTo(1);
            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).containsEntry("name", "Fireball");
        }

        @Test
        void blankQueryReturnsAllSpells() {
            createSpell("fireball", "Fireball", "{\"name\":\"Fireball\"}");
            createSpell("lightning-bolt", "Lightning Bolt", "{\"name\":\"Lightning Bolt\"}");

            Map<String, Object> result = spellService.searchSpells("");

            assertThat(result.get("count")).isEqualTo(2);
            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("relationship persistence")
    class RelationshipPersistenceTests {

        @Test
        void addSpellToCharacterPersistsRelationship() {
            User user = createUser("user1");
            PlayerCharacter character = createCharacter(user);
            Spell spell = createSpell("fireball", "Fireball", "{\"name\":\"Fireball\"}");

            CharacterSpellResponseDTO response = spellService.addSpellToCharacter(
                    character.getId(),
                    new SpellSaveInputDTO(spell.getSlug(), spell.getName()),
                    user.getId()
            );

            assertThat(response).isNotNull();

            PlayerCharacter reloaded = playerCharacterRepository.findById(character.getId()).orElseThrow();
            assertThat(reloaded.getSpells())
                    .extracting(Spell::getSlug)
                    .containsExactly(spell.getSlug());
        }

        @Test
        void removeSpellFromCharacterPersistsRemoval() {
            User user = createUser("user1");
            PlayerCharacter character = createCharacter(user);
            Spell spell = createSpell("fireball", "Fireball", "{\"name\":\"Fireball\"}");

            character.getSpells().add(spell);
            playerCharacterRepository.save(character);

            spellService.removeSpellFromCharacter(character.getId(), spell.getSlug(), user.getId());

            PlayerCharacter reloaded = playerCharacterRepository.findById(character.getId()).orElseThrow();
            assertThat(reloaded.getSpells()).isEmpty();
        }

        @Test
        void sameSpellCanBeAttachedToTwoDifferentCharacters() {
            User user = createUser("user1");
            PlayerCharacter character1 = createCharacter(user);
            PlayerCharacter character2 = createCharacter(user);
            Spell spell = createSpell("fireball", "Fireball", "{\"name\":\"Fireball\"}");

            spellService.addSpellToCharacter(
                    character1.getId(),
                    new SpellSaveInputDTO(spell.getSlug(), spell.getName()),
                    user.getId()
            );
            spellService.addSpellToCharacter(
                    character2.getId(),
                    new SpellSaveInputDTO(spell.getSlug(), spell.getName()),
                    user.getId()
            );

            PlayerCharacter reloaded1 = playerCharacterRepository.findById(character1.getId()).orElseThrow();
            PlayerCharacter reloaded2 = playerCharacterRepository.findById(character2.getId()).orElseThrow();

            assertThat(reloaded1.getSpells())
                    .extracting(Spell::getSlug)
                    .containsExactly(spell.getSlug());
            assertThat(reloaded2.getSpells())
                    .extracting(Spell::getSlug)
                    .containsExactly(spell.getSlug());
        }
    }

    @Nested
    @DisplayName("JSON deserialization")
    class JsonDeserializationTests {

        @Test
        void searchSpellsParsesRealJsonFields() {
            createSpell(
                    "fireball",
                    "Fireball",
                    "{\"name\":\"Fireball\",\"level\":3,\"school\":\"Evocation\"}"
            );

            Map<String, Object> result = spellService.searchSpells("fire");

            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            assertThat(results).hasSize(1);
            assertThat(results.get(0))
                    .containsEntry("name", "Fireball")
                    .containsEntry("level", 3)
                    .containsEntry("school", "Evocation");
        }

        @Test
        void malformedJsonReturnsFallbackShape() {
            createSpell("broken", "Broken Spell", "{not valid json");

            Map<String, Object> result = spellService.searchSpells("");

            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            assertThat(results).hasSize(1);
            assertThat(results.get(0))
                    .containsEntry("slug", "broken")
                    .containsEntry("name", "Broken Spell")
                    .containsEntry("error", "Failed to parse spell data");
        }
    }
}