package org.kruskopf.backend.offworlders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA converter that stores an Offworlders weapon list (a list of
 * {@link OffworldersWeapon}) as a JSON array in a single TEXT column.
 */
@Converter
public class OffworldersWeaponsConverter implements AttributeConverter<List<OffworldersWeapon>, String> {

    private static final TypeReference<List<OffworldersWeapon>> LIST_OF_WEAPONS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<OffworldersWeapon> weapons) {
        if (weapons == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(weapons);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert weapons to JSON", e);
        }
    }

    @Override
    public List<OffworldersWeapon> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, LIST_OF_WEAPONS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert JSON to weapons", e);
        }
    }
}
