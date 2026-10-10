package org.kruskopf.backend.ship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA converter that stores a ship's upgrade names as a JSON array of strings
 * in a single TEXT column. The catalog is small and players may repeat the two
 * repeatable upgrades, so a join table would be overkill.
 */
@Converter
public class ShipUpgradesConverter implements AttributeConverter<List<String>, String> {

    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> upgrades) {
        if (upgrades == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(upgrades);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert ship upgrades to JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, LIST_OF_STRINGS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert JSON to ship upgrades", e);
        }
    }
}
