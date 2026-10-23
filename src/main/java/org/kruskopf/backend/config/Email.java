package org.kruskopf.backend.config;


import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

public record Email(String email, boolean primary, boolean verified, String visibility) {

    public static List<Email> fromJson(String json) {
        try {
            ObjectMapper objectMapper = JsonMapper.builder().build();
            return objectMapper.readValue(json, new TypeReference<List<Email>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }
}
