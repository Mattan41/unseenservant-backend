package org.kruskopf.backend.playercharacter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PlayerCharacterStatsConverter implements AttributeConverter<PlayerCharacterStats, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(PlayerCharacterStats playerCharacterStats) {
        try {
            // Convert PlayerCharacterStats-object to JSON-string
            return objectMapper.writeValueAsString(playerCharacterStats);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert PlayerCharacterStats to JSON", e);
        }
    }

    @Override
    public PlayerCharacterStats convertToEntityAttribute(String dbData) {
        try {
            // Convert JSON-strings back to PlayerCharacterStats-object
            return objectMapper.readValue(dbData, PlayerCharacterStats.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert JSON to PlayerCharacterStats", e);
        }
    }

}

