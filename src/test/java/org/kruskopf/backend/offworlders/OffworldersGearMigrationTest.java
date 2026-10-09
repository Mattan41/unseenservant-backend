package org.kruskopf.backend.offworlders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import db.migration.V5__replace_offworlders_gear_with_items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the conversion embedded in the frozen {@code V5} migration.
 * <p>
 * V5 is immutable (its Flyway checksum must not change), so these assertions
 * intentionally describe V5's original output: weapon damage is empty and
 * {@code gear.notes} becomes an item named with the raw note text.
 * <p>
 * The corrected behaviour — a generic {@code Gear notes} item and weapon damage
 * prefilled from the legacy weapon type — lives in the frontend
 * {@code migrateGearToItems} helper, which upgrades any residual legacy payload.
 * In practice V5's conversion loop matches 0 rows on released installs, because
 * the Offworlders feature was never shipped with the legacy {@code gear} shape.
 */
@DisplayName("V5 gear -> items conversion")
class OffworldersGearMigrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("Converts weapons, armor and notes into free-form items")
    void convertsFullGear() throws Exception {
        String gear = "{\"primaryWeapon\":\"Snubnosed revolver\",\"primaryWeaponType\":\"Light\","
                + "\"secondaryWeapon\":\"Butterfly knife\",\"secondaryWeaponType\":\"Light\","
                + "\"armorType\":\"Heavy\",\"notes\":\"Band t-shirts\"}";

        JsonNode items = MAPPER.readTree(V5__replace_offworlders_gear_with_items.convertGearToItems(gear));

        assertThat(items).hasSize(4);
        assertThat(items.get(0).get("name").asText()).isEqualTo("Snubnosed revolver");
        assertThat(items.get(0).get("kind").asText()).isEqualTo("weapon");
        assertThat(items.get(0).get("damage").asText()).isEmpty();
        assertThat(items.get(1).get("name").asText()).isEqualTo("Butterfly knife");
        assertThat(items.get(2).get("name").asText()).isEqualTo("Heavy armor");
        assertThat(items.get(2).get("kind").asText()).isEqualTo("armor");
        assertThat(items.get(2).get("armorRating").asInt()).isEqualTo(2);
        assertThat(items.get(2).get("heavy").asBoolean()).isTrue();
        assertThat(items.get(3).get("name").asText()).isEqualTo("Band t-shirts");
        assertThat(items.get(3).get("kind").asText()).isEqualTo("item");
    }

    @Test
    @DisplayName("Skips empty fields and handles null/blank input")
    void handlesEmptyGear() throws Exception {
        assertThat(MAPPER.readTree(V5__replace_offworlders_gear_with_items.convertGearToItems(null))).isEmpty();
        assertThat(MAPPER.readTree(V5__replace_offworlders_gear_with_items.convertGearToItems(""))).isEmpty();
        assertThat(MAPPER.readTree(V5__replace_offworlders_gear_with_items.convertGearToItems("{}"))).isEmpty();
    }
}
