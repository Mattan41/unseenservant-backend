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
 * Replaces the Offworlders {@code gear} JSON block with a free-form {@code items}
 * array, in a single migration: add the column, backfill existing rows, drop the
 * old column.
 * <p>
 * This is a Java (rather than SQL) migration on purpose: parsing the old JSON in
 * SQL would be database-specific (MySQL {@code JSON_EXTRACT} vs H2
 * {@code JSON_VALUE}), whereas Jackson keeps it portable and unit-testable.
 * <p>
 * Backfill mapping (documented choice):
 * <ul>
 *   <li>{@code primaryWeapon} / {@code secondaryWeapon} → weapon items (damage left empty)</li>
 *   <li>{@code armorType} → an armor item (Light=1, Heavy=2, Assault=3; Heavy/Assault flagged heavy)</li>
 *   <li>{@code notes} → a generic item whose name is the note text</li>
 * </ul>
 */
public class V5__replace_offworlders_gear_with_items extends BaseJavaMigration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE offworlders_character_data ADD COLUMN items TEXT NULL");
        }

        try (Statement select = connection.createStatement();
             ResultSet rows = select.executeQuery("SELECT character_id, gear FROM offworlders_character_data");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE offworlders_character_data SET items = ? WHERE character_id = ?")) {
            while (rows.next()) {
                update.setString(1, convertGearToItems(rows.getString("gear")));
                update.setLong(2, rows.getLong("character_id"));
                update.addBatch();
            }
            update.executeBatch();
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE offworlders_character_data DROP COLUMN gear");
        }
    }

    /**
     * Converts a legacy {@code gear} JSON blob into an {@code items} JSON array.
     * Pure and side-effect free so it can be unit-tested.
     *
     * @param gearJson the legacy gear JSON (may be null/blank)
     * @return a JSON array string of items
     */
    public static String convertGearToItems(String gearJson) throws Exception {
        ArrayNode items = MAPPER.createArrayNode();
        if (gearJson != null && !gearJson.isBlank()) {
            JsonNode gear = MAPPER.readTree(gearJson);
            addItem(items, text(gear, "primaryWeapon"), "weapon", "", 0, false);
            addItem(items, text(gear, "secondaryWeapon"), "weapon", "", 0, false);
            addArmor(items, text(gear, "armorType"));
            addItem(items, text(gear, "notes"), "item", "", 0, false);
        }
        return MAPPER.writeValueAsString(items);
    }

    private static void addArmor(ArrayNode items, String armorType) {
        if (armorType == null || armorType.isBlank()) {
            return;
        }
        int rating = switch (armorType) {
            case "Light" -> 1;
            case "Heavy" -> 2;
            case "Assault" -> 3;
            default -> 0;
        };
        boolean heavy = "Heavy".equals(armorType) || "Assault".equals(armorType);
        addItem(items, armorType + " armor", "armor", "", rating, heavy);
    }

    private static void addItem(ArrayNode items, String name, String kind, String damage, int armorRating, boolean heavy) {
        if (name == null || name.isBlank()) {
            return;
        }
        ObjectNode item = items.addObject();
        item.put("name", name.trim());
        item.put("kind", kind);
        item.put("damage", damage);
        item.put("armorRating", armorRating);
        item.put("heavy", heavy);
        item.put("notes", "");
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
