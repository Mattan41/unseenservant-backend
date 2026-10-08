package org.kruskopf.backend.offworlders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OffworldersStatsConverter implements AttributeConverter<OffworldersStats, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(OffworldersStats stats) {
        try {
            // Convert OffworldersStats-object to JSON-string
            return objectMapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert OffworldersStats to JSON", e);
        }
    }

    @Override
    public OffworldersStats convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new OffworldersStats();
        }
        try {
            // Convert JSON-strings back to OffworldersStats-object
            return objectMapper.readValue(dbData, OffworldersStats.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to convert JSON to OffworldersStats", e);
        }
    }
}
