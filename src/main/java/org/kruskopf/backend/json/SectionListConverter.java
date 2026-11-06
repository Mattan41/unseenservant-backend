package org.kruskopf.backend.json;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.kruskopf.backend.section.ContentSection;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA converter that stores a list of {@link ContentSection}s as a JSON array in
 * a single TEXT column.
 * <p>
 * The same pattern as {@link StringListConverter}, for the one column shape that
 * is a list of small objects rather than a list of strings: the ship's and the
 * character's {@code sections}. The array order is the user's order and is
 * preserved verbatim — nothing here sorts.
 * <p>
 * A blank or missing column reads back as an empty list, so a row written before
 * the column existed loads as "no sections" rather than failing.
 */
@Converter
public class SectionListConverter implements AttributeConverter<List<ContentSection>, String> {

    private static final TypeReference<List<ContentSection>> LIST_OF_SECTIONS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(List<ContentSection> sections) {
        if (sections == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sections);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert section list to JSON", e);
        }
    }

    @Override
    public List<ContentSection> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, LIST_OF_SECTIONS);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to convert JSON to section list", e);
        }
    }
}
