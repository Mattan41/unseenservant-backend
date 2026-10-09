package db.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
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
 * Converts the Offworlders {@code skills} and {@code abilities} columns from JSON
 * arrays of strings into arrays of {@code { name, description }} objects, so a
 * custom skill/ability can carry a description.
 * <p>
 * Data-only migration (the columns are already TEXT). Java rather than SQL keeps
 * the JSON handling portable and unit-testable. It is also idempotent: elements
 * that are already objects are preserved.
 */
public class V6__offworlders_skills_abilities_as_entries extends BaseJavaMigration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement select = connection.createStatement();
             ResultSet rows = select.executeQuery(
                     "SELECT character_id, skills, abilities FROM offworlders_character_data");
             PreparedStatement update = connection.prepareStatement(
                     "UPDATE offworlders_character_data SET skills = ?, abilities = ? WHERE character_id = ?")) {
            while (rows.next()) {
                update.setString(1, toEntries(rows.getString("skills")));
                update.setString(2, toEntries(rows.getString("abilities")));
                update.setLong(3, rows.getLong("character_id"));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    /**
     * Converts a JSON array of strings (or already-objects) into an array of
     * {@code { name, description }} entries. Pure and side-effect free so it can
     * be unit-tested.
     *
     * @param json legacy JSON array (may be null/blank)
     * @return a JSON array string of entries
     */
    public static String toEntries(String json) throws Exception {
        ArrayNode entries = MAPPER.createArrayNode();
        if (json != null && !json.isBlank()) {
            JsonNode array;
            try {
                array = MAPPER.readTree(json);
            } catch (JsonProcessingException e) {
                // Unexpected non-JSON legacy value: keep the raw text as one entry
                // rather than failing the migration and blocking application startup.
                entries.add(entry(json.trim()));
                return MAPPER.writeValueAsString(entries);
            }
            if (array.isArray()) {
                for (JsonNode element : array) {
                    if (element.isTextual()) {
                        entries.add(entry(element.asText()));
                    } else if (element.isObject() && element.hasNonNull("name")) {
                        ObjectNode obj = entries.addObject();
                        obj.put("name", element.get("name").asText(""));
                        obj.put("description",
                                element.hasNonNull("description") ? element.get("description").asText("") : "");
                    }
                }
            }
        }
        return MAPPER.writeValueAsString(entries);
    }

    private static ObjectNode entry(String name) {
        ObjectNode obj = MAPPER.createObjectNode();
        obj.put("name", name);
        obj.put("description", "");
        return obj;
    }
}
