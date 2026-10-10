package db.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Reshapes the Offworlders inventory for 1.6, in a single migration.
 * <p>
 * The 1.5 {@code kind}-discriminated {@code items} array is split into a typed
 * {@code weapons} array ({@code { type, description }}) plus a free-text
 * {@code items} array ({@code { name, description }}). Any {@code kind: 'armor'}
 * entries are folded into the single {@code armor} value (highest rating wins).
 * <p>
 * Java (rather than SQL) for the same reason as {@code V5}: parsing and
 * rewriting JSON in SQL would be database-specific, whereas Jackson keeps it
 * portable and unit-testable.
 * <p>
 * Backfill mapping (documented choice):
 * <ul>
 *   <li>{@code kind: 'weapon'} → a weapon, {@code type} reverse-mapped from the
 *       legacy damage expression (1D6→Light, 1D6+1→Medium, 1D6+2→Heavy,
 *       anything else→Light), {@code description} from the legacy name/notes</li>
 *   <li>{@code kind: 'armor'} → folded into {@code armor} ({@code max} of the
 *       existing value and the entry rating), then dropped</li>
 *   <li>everything else → an item, {@code description} from the legacy notes</li>
 * </ul>
 */
public class V8__split_offworlders_weapons_and_items extends BaseJavaMigration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE offworlders_character_data ADD COLUMN weapons TEXT NULL");
        }

        try (Statement select = connection.createStatement();
             ResultSet rows = select.executeQuery("SELECT character_id, items, armor FROM offworlders_character_data");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE offworlders_character_data SET weapons = ?, items = ?, armor = ? WHERE character_id = ?")) {
            while (rows.next()) {
                Conversion conversion = convert(rows.getString("items"), rows.getInt("armor"));
                update.setString(1, conversion.weaponsJson());
                update.setString(2, conversion.itemsJson());
                update.setInt(3, conversion.armor());
                update.setLong(4, rows.getLong("character_id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    /** The new {@code weapons}/{@code items} JSON plus the resulting armor value for one row. */
    public record Conversion(String weaponsJson, String itemsJson, int armor) {
    }

    /**
     * Converts one row's legacy inventory. Pure and side-effect free so it can be
     * unit-tested.
     *
     * @param itemsJson the legacy {@code items} JSON array (may be null/blank)
     * @param armor     the row's existing armor value
     * @return the new weapons/items JSON and the clamped armor value
     */
    public static Conversion convert(String itemsJson, int armor) throws Exception {
        ArrayNode weapons = MAPPER.createArrayNode();
        ArrayNode items = MAPPER.createArrayNode();
        int bestArmor = armor;

        if (itemsJson != null && !itemsJson.isBlank()) {
            JsonNode array = MAPPER.readTree(itemsJson);
            if (array.isArray()) {
                for (JsonNode entry : array) {
                    if (!entry.isObject()) {
                        continue;
                    }
                    String kind = text(entry, "kind");
                    if ("armor".equals(kind)) {
                        bestArmor = Math.max(bestArmor, entry.path("armorRating").asInt(0));
                    } else if ("weapon".equals(kind)) {
                        ObjectNode weapon = weapons.addObject();
                        weapon.put("type", weaponTypeForDamage(text(entry, "damage")));
                        weapon.put("description", nonBlank(text(entry, "name"), text(entry, "notes")));
                    } else {
                        ObjectNode item = items.addObject();
                        item.put("name", text(entry, "name"));
                        item.put("description", nonBlank(text(entry, "description"), text(entry, "notes")));
                    }
                }
            }
        }

        bestArmor = Math.max(0, Math.min(3, bestArmor));
        return new Conversion(
                MAPPER.writeValueAsString(weapons),
                MAPPER.writeValueAsString(items),
                bestArmor);
    }

    /** Reverse-map a legacy damage expression to a weapon category (unknown → Light). */
    private static String weaponTypeForDamage(String damage) {
        return switch (damage == null ? "" : damage.trim()) {
            case "1D6+1" -> "Medium";
            case "1D6+2" -> "Heavy";
            default -> "Light";
        };
    }

    private static String nonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : (fallback != null ? fallback : "");
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
