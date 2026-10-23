package org.kruskopf.backend.ship;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA converter that stores a list of strings as a JSON array in a single TEXT
 * column. Used for the ship's upgrade names (small, closed catalog that may
 * contain duplicates for repeatable upgrades) and its gallery image URLs.
 */
@Converter
public class ShipStringListConverter implements AttributeConverter<List<String>, String> {

    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(List<String> values) {
        if (values == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert ship string list to JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, LIST_OF_STRINGS);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert JSON to ship string list", e);
        }
    }
}
