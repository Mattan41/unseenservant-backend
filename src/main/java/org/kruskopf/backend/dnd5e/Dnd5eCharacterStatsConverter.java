package org.kruskopf.backend.dnd5e;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class Dnd5eCharacterStatsConverter implements AttributeConverter<Dnd5eCharacterStats, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Dnd5eCharacterStats stats) {
        try {
            // Convert Dnd5eCharacterStats-object to JSON-string
            return objectMapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert Dnd5eCharacterStats to JSON", e);
        }
    }

    @Override
    public Dnd5eCharacterStats convertToEntityAttribute(String dbData) {
        try {
            // Convert JSON-strings back to Dnd5eCharacterStats-object
            return objectMapper.readValue(dbData, Dnd5eCharacterStats.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert JSON to Dnd5eCharacterStats", e);
        }
    }
}
