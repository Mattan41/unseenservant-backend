package org.kruskopf.backend.offworlders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import db.migration.V8__split_offworlders_weapons_and_items;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the conversion embedded in the {@code V8} migration: the 1.5
 * {@code kind}-discriminated {@code items} array becomes a typed {@code weapons}
 * array plus a free-text {@code items} array, with armor items folded into the
 * single {@code armor} value.
 */
@DisplayName("V8 items -> weapons + items split")
class OffworldersWeaponsMigrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("Splits weapons, folds armor into the armor value, and keeps items")
    void splitsLegacyInventory() throws Exception {
        String items = "["
                + "{\"name\":\"Snubnosed revolver\",\"kind\":\"weapon\",\"damage\":\"1D6\",\"armorRating\":0,\"heavy\":false,\"notes\":\"\"},"
                + "{\"name\":\"Heavy cannon\",\"kind\":\"weapon\",\"damage\":\"1D6+2\",\"armorRating\":0,\"heavy\":true,\"notes\":\"\"},"
                + "{\"name\":\"Light armor\",\"kind\":\"armor\",\"damage\":\"\",\"armorRating\":1,\"heavy\":false,\"notes\":\"\"},"
                + "{\"name\":\"Rope\",\"kind\":\"item\",\"damage\":\"\",\"armorRating\":0,\"heavy\":false,\"notes\":\"50 ft\"}"
                + "]";

        V8__split_offworlders_weapons_and_items.Conversion conversion =
                V8__split_offworlders_weapons_and_items.convert(items, 0);

        JsonNode weapons = MAPPER.readTree(conversion.weaponsJson());
        assertThat(weapons).hasSize(2);
        assertThat(weapons.get(0).get("type").asText()).isEqualTo("Light");
        assertThat(weapons.get(0).get("description").asText()).isEqualTo("Snubnosed revolver");
        assertThat(weapons.get(1).get("type").asText()).isEqualTo("Heavy");
        assertThat(weapons.get(1).get("description").asText()).isEqualTo("Heavy cannon");

        JsonNode newItems = MAPPER.readTree(conversion.itemsJson());
        assertThat(newItems).hasSize(1);
        assertThat(newItems.get(0).get("name").asText()).isEqualTo("Rope");
        assertThat(newItems.get(0).get("description").asText()).isEqualTo("50 ft");

        assertThat(conversion.armor()).isEqualTo(1);
    }

    @Test
    @DisplayName("Keeps the larger of the existing armor value and the armor item rating")
    void keepsHighestArmor() throws Exception {
        String items = "[{\"name\":\"Assault armor\",\"kind\":\"armor\",\"armorRating\":3}]";
        V8__split_offworlders_weapons_and_items.Conversion conversion =
                V8__split_offworlders_weapons_and_items.convert(items, 1);
        assertThat(conversion.armor()).isEqualTo(3);
    }

    @Test
    @DisplayName("Handles null/blank/empty input")
    void handlesEmptyInput() throws Exception {
        assertThat(MAPPER.readTree(V8__split_offworlders_weapons_and_items.convert(null, 2).weaponsJson()))
                .isEmpty();
        assertThat(MAPPER.readTree(V8__split_offworlders_weapons_and_items.convert("", 2).itemsJson()))
                .isEmpty();
        assertThat(V8__split_offworlders_weapons_and_items.convert("[]", 2).armor()).isEqualTo(2);
    }
}
