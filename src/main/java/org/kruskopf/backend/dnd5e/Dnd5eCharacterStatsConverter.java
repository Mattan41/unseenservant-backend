package org.kruskopf.backend.dnd5e;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Converter(autoApply = true)
public class Dnd5eCharacterStatsConverter implements AttributeConverter<Dnd5eCharacterStats, String> {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(Dnd5eCharacterStats stats) {
        try {
            // Convert Dnd5eCharacterStats-object to JSON-string
            return objectMapper.writeValueAsString(stats);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert Dnd5eCharacterStats to JSON", e);
        }
    }

    @Override
    public Dnd5eCharacterStats convertToEntityAttribute(String dbData) {
        try {
            // Convert JSON-strings back to Dnd5eCharacterStats-object
            return objectMapper.readValue(dbData, Dnd5eCharacterStats.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert JSON to Dnd5eCharacterStats", e);
        }
    }
}
