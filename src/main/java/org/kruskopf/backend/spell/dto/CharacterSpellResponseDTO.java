package org.kruskopf.backend.spell.dto;

import java.util.Map;

public record CharacterSpellResponseDTO(
        Long characterId,
        String slug,
        String name,
        Map<String, Object> spellData
) {
}
