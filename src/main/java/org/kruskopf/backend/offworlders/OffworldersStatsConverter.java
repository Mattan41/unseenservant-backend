package org.kruskopf.backend.offworlders;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Converter(autoApply = true)
public class OffworldersStatsConverter implements AttributeConverter<OffworldersStats, String> {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(OffworldersStats stats) {
        try {
            // Convert OffworldersStats-object to JSON-string
            return objectMapper.writeValueAsString(stats);
        } catch (JacksonException e) {
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
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert JSON to OffworldersStats", e);
        }
    }
}
