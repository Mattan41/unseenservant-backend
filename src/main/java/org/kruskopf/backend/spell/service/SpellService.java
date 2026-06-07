package org.kruskopf.backend.spell.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.kruskopf.backend.playercharacter.repository.PlayerCharacterRepository;
import org.kruskopf.backend.spell.dto.CharacterSpellResponseDTO;
import org.kruskopf.backend.spell.dto.SpellSaveInputDTO;
import org.kruskopf.backend.spell.entity.Spell;
import org.kruskopf.backend.spell.repository.SpellRepository;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Service
public class SpellService {

    private final SpellRepository spellRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SpellService(SpellRepository spellRepository,
                        PlayerCharacterRepository playerCharacterRepository,
                        RestClient restClient,
                        ObjectMapper objectMapper) {
        this.spellRepository = spellRepository;
        this.playerCharacterRepository = playerCharacterRepository;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CharacterSpellResponseDTO addSpellToCharacter(Long characterId, SpellSaveInputDTO input, Long userId) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        Spell spell = spellRepository.findById(input.slug())
                .orElseGet(() -> {
                    String raw = fetchSpellFromOpen5e(input.slug());
                    return spellRepository.saveAndFlush(new Spell(input.slug(), input.name(), raw));
                });

        character.getSpells().add(spell);
        playerCharacterRepository.save(character);

        return toResponseDTO(characterId, spell);
    }

    @Transactional(readOnly = true)
    public List<CharacterSpellResponseDTO> getSpellsForCharacter(Long characterId, Long userId) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        return character.getSpells().stream()
                .map(spell -> toResponseDTO(characterId, spell))
                .toList();
    }

    @Transactional
    public void removeSpellFromCharacter(Long characterId, String slug, Long userId) {
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));

        if (!character.getOwner().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User does not own this character");
        }

        boolean removed = character.getSpells().removeIf(s -> s.getSlug().equals(slug));
        if (!removed) {
            throw new ResourceNotFoundException("Spell '" + slug + "' not found on character " + characterId);
        }

        playerCharacterRepository.save(character);
    }

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 500))
    private String fetchSpellFromOpen5e(String slug) {
        try {
            return restClient.get()
                    .uri("https://api.open5e.com/v2/spells/{slug}/", slug)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new RuntimeException("Open5e API unavailable for slug '" + slug + "': " + e.getMessage(), e);
        }
    }

    private CharacterSpellResponseDTO toResponseDTO(Long characterId, Spell spell) {
        try {
            Map<String, Object> spellData = objectMapper.readValue(
                    spell.getRawJsonData(),
                    new TypeReference<>() {}
            );
            return new CharacterSpellResponseDTO(characterId, spell.getSlug(), spell.getName(), spellData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize spell data for slug '" + spell.getSlug() + "'", e);
        }
    }
}
