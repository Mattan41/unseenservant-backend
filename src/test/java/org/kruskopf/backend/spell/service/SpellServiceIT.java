package org.kruskopf.backend.spell.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.kruskopf.backend.testsupport.TestDataFactory;
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

    @Nested
    @DisplayName("findByNameContainingIgnoreCase")
    class FindByNameContainingIgnoreCaseTests {

        @Test
        void returnsMatchingSpellCaseInsensitive() {
            spellRepository.save(TestDataFactory.aSpell("fireball", "Fireball", "{\"name\":\"Fireball\"}"));
            spellRepository.save(TestDataFactory.aSpell("lightning‑bolt", "Lightning Bolt", "{\"name\":\"Lightning Bolt\"}"));
            Map<String, Object> result = spellService.searchSpells("fire", 0, 20);

            assertThat(result.get("count")).isEqualTo(1L);
            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            assertThat(results).hasSize(1);
            assertThat(results.getFirst()).containsEntry("name", "Fireball");
        }

        @Test
        void blankQueryReturnsNoResults() {
            spellRepository.save(TestDataFactory.aSpell("fireball", "Fireball", "{\"name\":\"Fireball\"}"));
            spellRepository.save(TestDataFactory.aSpell("lightning‑bolt", "Lightning Bolt", "{\"name\":\"Lightning Bolt\"}"));
            Map<String, Object> result = spellService.searchSpells("", 0, 20);

            assertThat(result.get("count")).isEqualTo(0);
            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            assertThat(results).isEmpty();
        }

        @Test
        void paginationReturnsCorrectPage() {
            // // Add spells
            spellRepository.save(TestDataFactory.aSpell("acid-arrow", "Acid Arrow", "{\"name\":\"Acid Arrow\"}"));
            spellRepository.save(TestDataFactory.aSpell("fireball", "Fireball", "{\"name\":\"Fireball\"}"));
            spellRepository.save(TestDataFactory.aSpell("ray-of-frost", "Ray of Frost", "{\"name\":\"Ray of Frost\"}"));
            spellRepository.save(TestDataFactory.aSpell("lightning-bolt", "Lightning Bolt", "{\"name\":\"Lightning Bolt\"}"));

            // First page, size 2
            Map<String, Object> page1 = spellService.searchSpells("a", 0, 2);
            assertThat(page1.get("count")).isEqualTo(3L);
            List<Map<String, Object>> results1 = (List<Map<String, Object>>) page1.get("results");
            assertThat(results1).hasSize(2);
            assertThat(results1.get(0)).containsEntry("name", "Acid Arrow");
            assertThat(results1.get(1)).containsEntry("name", "Fireball");

            // Second page, size 2
            Map<String, Object> page2 = spellService.searchSpells("a", 1, 2);
            List<Map<String, Object>> results2 = (List<Map<String, Object>>) page2.get("results");
            assertThat(results2).hasSize(1);
            assertThat(results2.get(0)).containsEntry("name", "Ray of Frost");
        }

        @Test
        void sizeGreaterThanDatasetReturnsAllResults() {
            // Add 150 spells
            for (int i = 0; i < 150; i++) {
                spellRepository.save(TestDataFactory.aSpell("spell-" + i, "Spell " + i, "{\"name\":\"Spell " + i + "\"}"));
            }

            Map<String, Object> result = spellService.searchSpells("spell", 0, 200);
            assertThat(result.get("count")).isEqualTo(150L);
            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            assertThat(results).hasSize(150);
        }
    }

    @Nested
    @DisplayName("relationship persistence")
    class RelationshipPersistenceTests {

        @Test
        void addSpellToCharacterPersistsRelationship() {
            User user = TestDataFactory.aUser(userRepository, "user1");
            PlayerCharacter character = playerCharacterRepository.save(TestDataFactory.aCharacter(user));
            Spell spell = spellRepository.save(TestDataFactory.aSpell("fireball", "Fireball", "{\"name\":\"Fireball\"}"));

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
        void removeSpellFromCharacterPersitsRemoval() {
            User user = TestDataFactory.aUser(userRepository, "user1");
            PlayerCharacter character = playerCharacterRepository.save(TestDataFactory.aCharacter(user));
            Spell spell = spellRepository.save(TestDataFactory.aSpell("fireball", "Fireball", "{\"name\":\"Fireball\"}"));

            character.getSpells().add(spell);
            playerCharacterRepository.save(character);

            spellService.removeSpellFromCharacter(character.getId(), spell.getSlug(), user.getId());

            PlayerCharacter reloaded = playerCharacterRepository.findById(character.getId()).orElseThrow();
            assertThat(reloaded.getSpells()).isEmpty();
        }

        @Test
        void sameSpellCanBeAttachedToTwoDifferentCharacters() {
            User user = TestDataFactory.aUser(userRepository, "user1");
            PlayerCharacter character1 = playerCharacterRepository.save(TestDataFactory.aCharacter(user));
            PlayerCharacter character2 = playerCharacterRepository.save(TestDataFactory.aCharacter(user));
            Spell spell = spellRepository.save(TestDataFactory.aSpell("fireball", "Fireball", "{\"name\":\"Fireball\"}"));

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
    @DisplayName("JSON deserialisation")
    class JsonDeserialisationTests {

        @Test
        void searchSpellsParsesRealJsonFields() {
            spellRepository.save(TestDataFactory.aSpell(
                    "fireball",
                    "Fireball",
                    "{\"name\":\"Fireball\",\"level\":3,\"school\":\"Evocation\"}"
            ));

            Map<String, Object> result = spellService.searchSpells("fire", 0, 20);

            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            assertThat(results).hasSize(1);
            assertThat(results.getFirst())
                    .containsEntry("name", "Fireball")
                    .containsEntry("level", 3)
                    .containsEntry("school", "Evocation");
        }

        @Test
        void malformedJsonReturnsFallbackShape() {
            spellRepository.save(TestDataFactory.aSpell("broken", "Broken Spell", "{not valid json"));

            Map<String, Object> result = spellService.searchSpells("broken", 0, 20);

            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            assertThat(results).hasSize(1);
            assertThat(results.getFirst())
                    .containsEntry("slug", "broken")
                    .containsEntry("name", "Broken Spell")
                    .containsEntry("error", "Failed to parse spell data");
        }
    }
}
