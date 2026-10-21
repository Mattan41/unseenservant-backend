package org.kruskopf.backend.offworlders;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA converter that stores a list of {@link OffworldersEntry} (skills or
 * abilities) as a JSON array in a single TEXT column.
 */
@Converter
public class OffworldersEntriesConverter implements AttributeConverter<List<OffworldersEntry>, String> {

    private static final TypeReference<List<OffworldersEntry>> LIST_OF_ENTRIES = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(List<OffworldersEntry> entries) {
        if (entries == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(entries);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert entries to JSON", e);
        }
    }

    @Override
    public List<OffworldersEntry> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, LIST_OF_ENTRIES);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert JSON to entries", e);
        }
    }
}
