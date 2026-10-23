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
 * JPA converter that stores an Offworlders inventory (a list of
 * {@link OffworldersItem}) as a JSON array in a single TEXT column.
 */
@Converter
public class OffworldersItemsConverter implements AttributeConverter<List<OffworldersItem>, String> {

    private static final TypeReference<List<OffworldersItem>> LIST_OF_ITEMS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(List<OffworldersItem> items) {
        if (items == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert items to JSON", e);
        }
    }

    @Override
    public List<OffworldersItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, LIST_OF_ITEMS);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert JSON to items", e);
        }
    }
}
