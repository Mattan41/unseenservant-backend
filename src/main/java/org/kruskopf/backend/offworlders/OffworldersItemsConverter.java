package org.kruskopf.backend.offworlders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<OffworldersItem> items) {
        if (items == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
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
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert JSON to items", e);
        }
    }
}
