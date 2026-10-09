package org.kruskopf.backend.offworlders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import db.migration.V6__offworlders_skills_abilities_as_entries;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("V6 skills/abilities -> entries conversion")
class OffworldersEntryMigrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("Converts a string array into name/description entries")
    void convertsStrings() throws Exception {
        JsonNode entries = MAPPER.readTree(
                V6__offworlders_skills_abilities_as_entries.toEntries("[\"Pilot\",\"Sneak\"]"));

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).get("name").asText()).isEqualTo("Pilot");
        assertThat(entries.get(0).get("description").asText()).isEmpty();
        assertThat(entries.get(1).get("name").asText()).isEqualTo("Sneak");
    }

    @Test
    @DisplayName("Preserves existing object entries (idempotent)")
    void preservesObjects() throws Exception {
        JsonNode entries = MAPPER.readTree(V6__offworlders_skills_abilities_as_entries.toEntries(
                "[{\"name\":\"X\",\"description\":\"does X\"}]"));

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).get("name").asText()).isEqualTo("X");
        assertThat(entries.get(0).get("description").asText()).isEqualTo("does X");
    }

    @Test
    @DisplayName("Falls back to a single entry for non-JSON legacy values")
    void handlesNonJson() throws Exception {
        JsonNode entries = MAPPER.readTree(V6__offworlders_skills_abilities_as_entries.toEntries("Pilot"));

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).get("name").asText()).isEqualTo("Pilot");
        assertThat(entries.get(0).get("description").asText()).isEmpty();
    }

    @Test
    @DisplayName("Handles null / blank / empty input")
    void handlesEmpty() throws Exception {
        assertThat(MAPPER.readTree(V6__offworlders_skills_abilities_as_entries.toEntries(null))).isEmpty();
        assertThat(MAPPER.readTree(V6__offworlders_skills_abilities_as_entries.toEntries(""))).isEmpty();
        assertThat(MAPPER.readTree(V6__offworlders_skills_abilities_as_entries.toEntries("[]"))).isEmpty();
        assertThat(MAPPER.readTree(V6__offworlders_skills_abilities_as_entries.toEntries("{}"))).isEmpty();
    }
}
