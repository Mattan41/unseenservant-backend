package org.kruskopf.backend.playercharacter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.kruskopf.backend.dnd5e.dto.Dnd5eCharacterDataInputDTO;
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
            assertThat(dto.dnd5e()).isNull();

            OffworldersCharacterDataOutputDTO offworlders = dto.offworlders();
            assertThat(offworlders).isNotNull();
            assertThat(offworlders.characterClass()).isEqualTo("Outlaw");
            assertThat(offworlders.species()).isEqualTo("Human");
            assertThat(offworlders.look()).isEqualTo("Weathered spacer");
            assertThat(offworlders.xp()).isEqualTo(3);
            assertThat(offworlders.health()).isEqualTo(15);
            assertThat(offworlders.armor()).isEqualTo(1);
            assertThat(offworlders.supply()).isEqualTo(2);
            assertThat(offworlders.supplyMax()).isEqualTo(3);
            assertThat(offworlders.credits()).isEqualTo(10);
            assertThat(offworlders.gear().getPrimaryWeapon()).isEqualTo("Snubnosed revolver");
            assertThat(offworlders.gear().getArmorType()).isEqualTo("Light");
            assertThat(offworlders.stats().getStrength()).isEqualTo(1);
            assertThat(offworlders.stats().getAgility()).isEqualTo(2);
            assertThat(offworlders.stats().getIntelligence()).isZero();
            assertThat(offworlders.stats().getWillpower()).isEqualTo(-1);
            assertThat(offworlders.skills()).containsExactly("Pilot", "Sneak");
            assertThat(offworlders.abilities()).containsExactly("Lucky");
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
                    new OffworldersCharacterDataInputDTO(
                            "Psychic",
                            "Human",
                            "Sharp eyes",
                            2,
                            13,
                            0,
                            2,
                            3,
                            10,
                            new OffworldersStats(0, 1, 3, 2),
                            List.of("Telekinesis"),
                            List.of("Blast", "Jump"),
                            null
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
            assertThat(character.getOffworldersData().getSkills()).containsExactly("Telekinesis");
            assertThat(character.getOffworldersData().getAbilities()).containsExactly("Blast", "Jump");
        }

        @Test
        @DisplayName("Creates an empty Offworlders block when the payload is omitted")
        void createsEmptyOffworldersBlock() {
            // Arrange
            User owner = TestDataFactory.aUser("player");
            CharacterInputDTO dto = new CharacterInputDTO(
                    null, null, "Nova", GameSystem.OFFWORLDERS, null, null, null, null);

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
                    new Dnd5eCharacterDataInputDTO(5, "Fighter", "Human", 44, 17, null),
                    new OffworldersCharacterDataInputDTO(
                            "Outlaw", null, null, null, null, null, null, null, null, null, null, null, null)
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
                    new OffworldersCharacterDataInputDTO(
                            null, null, null, 9, null, 3, null, null, null, null,
                            List.of("Pilot", "Sneak", "Scan"), null, null)
            );

            // Act
            mapper.patchEntity(character, dto);

            // Assert
            assertThat(character.getOffworldersData().getXp()).isEqualTo(9);
            assertThat(character.getOffworldersData().getArmor()).isEqualTo(3);
            assertThat(character.getOffworldersData().getSkills())
                    .containsExactly("Pilot", "Sneak", "Scan");
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
                    null, null, "Renamed", null, null, null, null,
                    new OffworldersCharacterDataInputDTO(
                            "Outlaw", null, null, null, null, null, null, null, null, null, null, null, null));

            // Act
            mapper.patchEntity(character, dto);

            // Assert
            assertThat(character.getName()).isEqualTo("Renamed");
            assertThat(character.getOffworldersData()).isNull();
        }
    }
}
