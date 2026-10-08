package org.kruskopf.backend.spell.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.dnd5e.entity.Dnd5eCharacterData;
import org.kruskopf.backend.playercharacter.entity.GameCharacter;
import org.kruskopf.backend.playercharacter.repository.GameCharacterRepository;
import org.kruskopf.backend.spell.dto.CharacterSpellResponseDTO;
import org.kruskopf.backend.spell.dto.SpellSaveInputDTO;
import org.kruskopf.backend.spell.entity.Spell;
import org.kruskopf.backend.spell.repository.SpellRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SpellService {

    private final SpellRepository spellRepository;
    private final GameCharacterRepository gameCharacterRepository;
    private final CampaignPermissionService campaignPermissionService;
    private final ObjectMapper objectMapper;

    public SpellService(SpellRepository spellRepository,
                        GameCharacterRepository gameCharacterRepository,
                        CampaignPermissionService campaignPermissionService,
                        ObjectMapper objectMapper) {
        this.spellRepository = spellRepository;
        this.gameCharacterRepository = gameCharacterRepository;
        this.campaignPermissionService = campaignPermissionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Searches spells in local database
     * @param query Optional search term to filter spells by name
     * @return Map containing count and results list with full spell data
     */
    public Map<String, Object> searchSpells(String query, int page, int size) {
        // Treat blank query as matching nothing
        if (query == null || query.isBlank()) {
            return Map.of(
                "count", 0,
                "results", List.of()
            );
        }

        Page<Spell> spellPage = spellRepository.findByNameContainingIgnoreCase(
            query, 
            PageRequest.of(page, size, Sort.by("name").ascending())
        );

        List<Map<String, Object>> spellResults = spellPage.getContent().stream()
                .map(this::parseSpellData)
                .toList();

        return Map.of(
                "count", spellPage.getTotalElements(),
                "results", spellResults
        );
    }

    @Transactional
    public CharacterSpellResponseDTO addSpellToCharacter(Long characterId, SpellSaveInputDTO input, Long userId) {
        GameCharacter character = gameCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        Spell spell = spellRepository.findById(input.slug())
                .orElseThrow(() -> new ResourceNotFoundException("Spell not found with slug: " + input.slug()));

        Dnd5eCharacterData dnd5eData = requireDnd5eData(character);
        dnd5eData.getSpells().add(spell);
        gameCharacterRepository.save(character);

        return toResponseDTO(characterId, spell);
    }

    @Transactional(readOnly = true)
    public List<CharacterSpellResponseDTO> getSpellsForCharacter(Long characterId, Long userId) {
        GameCharacter character = gameCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        if (!campaignPermissionService.canViewCharacter(character, userId)) {
            throw new UnauthorizedAccessException("User is not the owner of the character, nor GM of the campaign");
        }

        Dnd5eCharacterData dnd5eData = character.getDnd5eData();
        if (dnd5eData == null) {
            return List.of();
        }

        return dnd5eData.getSpells().stream()
                .map(spell -> toResponseDTO(characterId, spell))
                .toList();
    }

    public Map<String, Object> getSpellBySlug(String slug) {
        Spell spell = spellRepository.findById(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Spell not found with slug: " + slug));
        return parseSpellData(spell);
    }

    @Transactional
    public void removeSpellFromCharacter(Long characterId, String slug, Long userId) {
        GameCharacter character = gameCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        Dnd5eCharacterData dnd5eData = requireDnd5eData(character);
        boolean removed = dnd5eData.getSpells().removeIf(s -> s.getSlug().equals(slug));
        if (!removed) {
            throw new ResourceNotFoundException("Spell '" + slug + "' not found on character " + characterId);
        }

        gameCharacterRepository.save(character);
    }

    /**
     * Resolves the D&amp;D 5e data block for a character, failing if the character
     * has no associated D&amp;D 5e data.
     */
    private Dnd5eCharacterData requireDnd5eData(GameCharacter character) {
        Dnd5eCharacterData data = character.getDnd5eData();
        if (data == null) {
            throw new ResourceNotFoundException("Character " + character.getId() + " has no D&D 5e data");
        }
        return data;
    }

    private Map<String, Object> deserializeSpellJson(Spell spell) throws Exception {
        return objectMapper.readValue(spell.getRawJsonData(), new TypeReference<>() {});
    }

    private Map<String, Object> parseSpellData(Spell spell) {
        try {
            return deserializeSpellJson(spell);
        } catch (Exception e) {
            return Map.of(
                    "slug", spell.getSlug(),
                    "name", spell.getName(),
                    "error", "Failed to parse spell data"
            );
        }
    }

    private CharacterSpellResponseDTO toResponseDTO(Long characterId, Spell spell) {
        try {
            Map<String, Object> spellData = deserializeSpellJson(spell);
            return new CharacterSpellResponseDTO(characterId, spell.getSlug(), spell.getName(), spellData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize spell data for slug '" + spell.getSlug() + "'", e);
        }
    }
}
