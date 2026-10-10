package org.kruskopf.backend.playercharacter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataInputDTO;
import org.kruskopf.backend.offworlders.OffworldersEntry;
import org.kruskopf.backend.offworlders.OffworldersStats;
import org.kruskopf.backend.offworlders.dto.OffworldersCharacterDataInputDTO;
import org.kruskopf.backend.offworlders.dto.OffworldersCharacterDataOutputDTO;
import org.kruskopf.backend.playercharacter.dto.CharacterInputDTO;
import org.kruskopf.backend.playercharacter.dto.CharacterOutputDTO;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;
import org.kruskopf.backend.playercharacter.entity.GameSystem;
import org.kruskopf.backend.testsupport.TestDataFactory;
import org.kruskopf.backend.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameCharacterMapper Unit Tests")
class GameCharacterMapperTest {

    private final GameCharacterMapper mapper = new GameCharacterMapper();

    @Nested
    @DisplayName("toOutputDTO()")
    class ToOutputDTO {

        @Test
        @DisplayName("Maps the Offworlders block and leaves dnd5e null")
        void mapsOffworldersBlock() {
            // Arrange
            User owner = TestDataFactory.aUser("player");
            ReflectionTestUtils.setField(owner, "id", 7L);
            GameCharacter character = TestDataFactory.anOffworldersCharacter(owner);
            ReflectionTestUtils.setField(character, "id", 42L);

            // Act
            CharacterOutputDTO dto = mapper.toOutputDTO(character);

            // Assert
            assertThat(dto.systemType()).isEqualTo(GameSystem.OFFWORLDERS);
            assertThat(dto.appearance()).isEqualTo("Weathered spacer");
            assertThat(dto.dnd5e()).isNull();

            OffworldersCharacterDataOutputDTO offworlders = dto.offworlders();
            assertThat(offworlders).isNotNull();
            assertThat(offworlders.characterClass()).isEqualTo("Outlaw");
            assertThat(offworlders.species()).isEqualTo("Human");
            assertThat(offworlders.xp()).isEqualTo(3);
            assertThat(offworlders.health()).isEqualTo(15);
            assertThat(offworlders.armor()).isEqualTo(1);
            assertThat(offworlders.supply()).isEqualTo(2);
            assertThat(offworlders.supplyMax()).isEqualTo(3);
            assertThat(offworlders.credits()).isEqualTo(10);
            assertThat(offworlders.weapons()).hasSize(1);
            assertThat(offworlders.weapons().get(0).getType()).isEqualTo("Light");
            assertThat(offworlders.weapons().get(0).getDescription()).isEqualTo("Snubnosed revolver");
            assertThat(offworlders.items()).hasSize(1);
            assertThat(offworlders.items().get(0).getName()).isEqualTo("Band t-shirts");
            assertThat(offworlders.items().get(0).getDescription()).isEqualTo("Rotating collection.");
            assertThat(offworlders.stats().getStrength()).isEqualTo(1);
            assertThat(offworlders.stats().getAgility()).isEqualTo(2);
            assertThat(offworlders.stats().getIntelligence()).isZero();
            assertThat(offworlders.stats().getWillpower()).isEqualTo(-1);
            assertThat(offworlders.skills()).extracting(OffworldersEntry::getName)
                    .containsExactly("Pilot", "Sneak");
            assertThat(offworlders.abilities()).extracting(OffworldersEntry::getName)
                    .containsExactly("Lucky");
        }

        @Test
        @DisplayName("Withholds the private backstory unless explicitly allowed")
        void withholdsPrivateBackstoryByDefault() {
            // Arrange
            User owner = TestDataFactory.aUser("player");
            ReflectionTestUtils.setField(owner, "id", 7L);
            GameCharacter character = TestDataFactory.anOffworldersCharacter(owner);

            // Assert: the public backstory is always present…
            assertThat(mapper.toOutputDTO(character).backstory()).isEqualTo("Public backstory");
            // …but the private one only when the caller opts in.
            assertThat(mapper.toOutputDTO(character).privateBackstory()).isNull();
            assertThat(mapper.toOutputDTO(character, true).privateBackstory()).isEqualTo("Private backstory");
        }

        @Test
        @DisplayName("Maps the D&D 5e block and leaves offworlders null")
        void mapsDnd5eBlock() {
            // Arrange
            User owner = TestDataFactory.aUser("player");
            ReflectionTestUtils.setField(owner, "id", 7L);
            GameCharacter character = TestDataFactory.aCharacter(owner);
            ReflectionTestUtils.setField(character, "id", 42L);

            // Act
            CharacterOutputDTO dto = mapper.toOutputDTO(character);

            // Assert
            assertThat(dto.systemType()).isEqualTo(GameSystem.DND5E);
            assertThat(dto.dnd5e()).isNotNull();
            assertThat(dto.offworlders()).isNull();
        }
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {

        @Test
        @DisplayName("Attaches Offworlders data when systemType is OFFWORLDERS")
        void attachesOffworldersData() {
            // Arrange
            User owner = TestDataFactory.aUser("player");
            CharacterInputDTO dto = new CharacterInputDTO(
                    null,
                    null,
                    "Nova",
                    GameSystem.OFFWORLDERS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new OffworldersCharacterDataInputDTO(
                            "Psychic",
                            "Human",
                            2,
                            13,
                            0,
                            2,
                            3,
                            10,
                            new OffworldersStats(0, 1, 3, 2),
                            List.of(new OffworldersEntry("Telekinesis", "")),
                            List.of(new OffworldersEntry("Blast", ""), new OffworldersEntry("Jump", "")),
                            null,
                            null,
                            9,
                            2
                    )
            );

            // Act
            GameCharacter character = mapper.toEntity(dto, owner, null);

            // Assert
            assertThat(character.getSystemType()).isEqualTo(GameSystem.OFFWORLDERS);
            assertThat(character.getDnd5eData()).isNull();
            assertThat(character.getOffworldersData()).isNotNull();
            assertThat(character.getOffworldersData().getCharacter()).isSameAs(character);
            assertThat(character.getOffworldersData().getCharacterClass()).isEqualTo("Psychic");
            assertThat(character.getOffworldersData().getCredits()).isEqualTo(10);
            assertThat(character.getOffworldersData().getStats().getIntelligence()).isEqualTo(3);
            assertThat(character.getOffworldersData().getSkills())
                    .extracting(OffworldersEntry::getName)
                    .containsExactly("Telekinesis");
            assertThat(character.getOffworldersData().getAbilities())
                    .extracting(OffworldersEntry::getName)
                    .containsExactly("Blast", "Jump");
            assertThat(character.getOffworldersData().getCurrentHealth()).isEqualTo(9);
            assertThat(character.getOffworldersData().getHealthModifier()).isEqualTo(2);
        }

        @Test
        @DisplayName("Creates an empty Offworlders block when the payload is omitted")
        void createsEmptyOffworldersBlock() {
            // Arrange
            User owner = TestDataFactory.aUser("player");
            CharacterInputDTO dto = new CharacterInputDTO(
                    null, null, "Nova", GameSystem.OFFWORLDERS, null, null, null, null, null, null, null);

            // Act
            GameCharacter character = mapper.toEntity(dto, owner, null);

            // Assert
            assertThat(character.getOffworldersData()).isNotNull();
            assertThat(character.getOffworldersData().getCharacterClass()).isEmpty();
            assertThat(character.getOffworldersData().getHealth()).isEqualTo(12);
            assertThat(character.getOffworldersData().getSkills()).isEmpty();
            assertThat(character.getOffworldersData().getAbilities()).isEmpty();
        }

        @Test
        @DisplayName("Still attaches D&D 5e data and ignores the offworlders block for DND5E")
        void dnd5eUnaffected() {
            // Arrange
            User owner = TestDataFactory.aUser("player");
            CharacterInputDTO dto = new CharacterInputDTO(
                    null,
                    null,
                    "Aragorn",
                    GameSystem.DND5E,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new Dnd5eCharacterDataInputDTO(5, "Fighter", "Human", 44, 17, null),
                    new OffworldersCharacterDataInputDTO(
                            "Outlaw", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
            );

            // Act
            GameCharacter character = mapper.toEntity(dto, owner, null);

            // Assert
            assertThat(character.getDnd5eData()).isNotNull();
            assertThat(character.getDnd5eData().getCharacterClass()).isEqualTo("Fighter");
            assertThat(character.getOffworldersData()).isNull();
        }
    }

    @Nested
    @DisplayName("patchEntity()")
    class PatchEntity {

        @Test
        @DisplayName("Patches Offworlders fields field-by-field")
        void patchesOffworldersFields() {
            // Arrange
            User owner = TestDataFactory.aUser("player");
            GameCharacter character = TestDataFactory.anOffworldersCharacter(owner);
            CharacterInputDTO dto = new CharacterInputDTO(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new OffworldersCharacterDataInputDTO(
                            null, null, 9, null, 3, null, null, null, null,
                            List.of(new OffworldersEntry("Pilot", ""), new OffworldersEntry("Sneak", ""), new OffworldersEntry("Scan", "")), null, null, null, 5, 3)
            );

            // Act
            mapper.patchEntity(character, dto);

            // Assert
            assertThat(character.getOffworldersData().getXp()).isEqualTo(9);
            assertThat(character.getOffworldersData().getArmor()).isEqualTo(3);
            assertThat(character.getOffworldersData().getSkills())
                    .extracting(OffworldersEntry::getName)
                    .containsExactly("Pilot", "Sneak", "Scan");
            assertThat(character.getOffworldersData().getCurrentHealth()).isEqualTo(5);
            assertThat(character.getOffworldersData().getHealthModifier()).isEqualTo(3);
            // Untouched fields keep their original values.
            assertThat(character.getOffworldersData().getCharacterClass()).isEqualTo("Outlaw");
            assertThat(character.getOffworldersData().getHealth()).isEqualTo(15);
        }

        @Test
        @DisplayName("Ignores the offworlders block on a D&D 5e character")
        void ignoresOffworldersOnDnd5eCharacter() {
            // Arrange
            User owner = TestDataFactory.aUser("player");
            GameCharacter character = TestDataFactory.aCharacter(owner);
            CharacterInputDTO dto = new CharacterInputDTO(
                    null, null, "Renamed", null, null, null, null, null, null, null,
                    new OffworldersCharacterDataInputDTO(
                            "Outlaw", null, null, null, null, null, null, null, null, null, null, null, null, null, null));

            // Act
            mapper.patchEntity(character, dto);

            // Assert
            assertThat(character.getName()).isEqualTo("Renamed");
            assertThat(character.getOffworldersData()).isNull();
        }
    }
}
