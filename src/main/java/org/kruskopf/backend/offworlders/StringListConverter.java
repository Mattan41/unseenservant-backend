package org.kruskopf.backend.offworlders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic JPA converter that stores a {@code List<String>} as a JSON array in a
 * single TEXT column.
 * <p>
 * Used for freeform-friendly, checklist-style values such as Offworlders skills
 * and abilities: the UI offers a canonical catalog but users may store custom
 * entries too, so a plain ordered string list is the right granularity.
 * <p>
 * Deliberately not marked {@code autoApply} so that it only applies to fields
 * explicitly annotated with {@code @Convert}.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final TypeReference<List<String>> LIST_OF_STRING = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert List<String> to JSON", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, LIST_OF_STRING);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert JSON to List<String>", e);
        }
    }
}
