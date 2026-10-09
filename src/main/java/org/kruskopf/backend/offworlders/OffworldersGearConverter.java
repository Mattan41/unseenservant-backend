package org.kruskopf.backend.offworlders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter that stores an {@link OffworldersGear} block as JSON in a single
 * TEXT column. Mirrors {@link OffworldersStatsConverter}.
 */
@Converter
public class OffworldersGearConverter implements AttributeConverter<OffworldersGear, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(OffworldersGear gear) {
        try {
            return objectMapper.writeValueAsString(gear);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert OffworldersGear to JSON", e);
        }
    }

    @Override
    public OffworldersGear convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new OffworldersGear();
        }
        try {
            return objectMapper.readValue(dbData, OffworldersGear.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert JSON to OffworldersGear", e);
        }
    }
}
