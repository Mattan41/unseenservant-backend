package org.kruskopf.backend.ship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA converter that stores a crew member's characters (a list of
 * {@link ShipCrewCharacter}) as a JSON array in a single TEXT column.
 */
@Converter
public class ShipCrewCharactersConverter implements AttributeConverter<List<ShipCrewCharacter>, String> {

    private static final TypeReference<List<ShipCrewCharacter>> LIST_OF_CHARACTERS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<ShipCrewCharacter> characters) {
        if (characters == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(characters);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert ship crew characters to JSON", e);
        }
    }

    @Override
    public List<ShipCrewCharacter> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, LIST_OF_CHARACTERS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert JSON to ship crew characters", e);
        }
    }
}
