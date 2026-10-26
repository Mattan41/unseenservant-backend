package org.kruskopf.backend.json;

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
 * column.
 * <p>
 * Shared by every "small list of strings" column in the schema: the ship's
 * upgrade names (a small, closed catalog that may repeat an entry for repeatable
 * upgrades), gallery image URLs — the ship's and a character's — and any future
 * column of the same shape.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

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
            throw new IllegalStateException("Failed to convert string list to JSON", e);
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
            throw new IllegalStateException("Failed to convert JSON to string list", e);
        }
    }
}
