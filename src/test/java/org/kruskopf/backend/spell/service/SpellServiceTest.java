package org.kruskopf.backend.spell.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.playercharacter.entity.PlayerCharacter;
import org.kruskopf.backend.playercharacter.repository.PlayerCharacterRepository;
import org.kruskopf.backend.spell.dto.CharacterSpellResponseDTO;
import org.kruskopf.backend.spell.dto.SpellSaveInputDTO;
import org.kruskopf.backend.spell.entity.Spell;
import org.kruskopf.backend.spell.repository.SpellRepository;
import org.kruskopf.backend.user.entity.User;

@ExtendWith(MockitoExtension.class)
class SpellServiceTest {

    @Mock
    private SpellRepository spellRepository;

    @Mock
    private PlayerCharacterRepository playerCharacterRepository;

    @Mock
    private CampaignPermissionService campaignPermissionService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SpellService spellService;

    private final Long characterId = 1L;
    private final Long userId = 10L;
    private final String slug = "fireball";
    private final String name = "Fireball";

    @Test
    void searchSpells_blankQuery_returnsAllSpells() throws Exception {
        Spell spell1 = mock(Spell.class);
        Spell spell2 = mock(Spell.class);
        when(spell1.getRawJsonData()).thenReturn("json1");
        when(spell2.getRawJsonData()).thenReturn("json2");
        when(spellRepository.findAll()).thenReturn(List.of(spell1, spell2));
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(Map.of("name", "Fireball"));

        Map<String, Object> result = spellService.searchSpells("");

        assertEquals(2, result.get("count"));
        assertEquals(2, ((List<?>) result.get("results")).size());
        verify(spellRepository).findAll();
        verify(spellRepository, never()).findByNameContainingIgnoreCase(anyString());
    }

    @Test
    void searchSpells_withQuery_usesFindByNameContainingIgnoreCase() throws Exception {
        Spell spell = mock(Spell.class);
        when(spell.getRawJsonData()).thenReturn("json");
        when(spellRepository.findByNameContainingIgnoreCase("fire")).thenReturn(List.of(spell));
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(Map.of("name", "Fireball"));

        Map<String, Object> result = spellService.searchSpells("fire");

        assertEquals(1, result.get("count"));
        assertEquals(1, ((List<?>) result.get("results")).size());
        verify(spellRepository).findByNameContainingIgnoreCase("fire");
        verify(spellRepository, never()).findAll();
    }

    @Test
    void searchSpells_parseError_returnsFallbackSpellData() throws Exception {
        Spell badSpell = mock(Spell.class);
        Spell goodSpell = mock(Spell.class);

        when(badSpell.getRawJsonData()).thenReturn("bad-json");
        when(badSpell.getSlug()).thenReturn("bad-slug");
        when(badSpell.getName()).thenReturn("Bad Spell");

        when(goodSpell.getRawJsonData()).thenReturn("good-json");

        when(spellRepository.findAll()).thenReturn(List.of(badSpell, goodSpell));
        when(objectMapper.readValue(eq("bad-json"), any(TypeReference.class)))
                .thenThrow(new RuntimeException("Parse failure"));
        when(objectMapper.readValue(eq("good-json"), any(TypeReference.class)))
                .thenReturn(Map.of("name", "Good Spell"));

        Map<String, Object> result = spellService.searchSpells("");

        assertEquals(2, result.get("count"));
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
        assertEquals(2, results.size());

        Map<String, Object> fallback = results.stream()
                .filter(m -> "bad-slug".equals(m.get("slug")))
                .findFirst()
                .orElseThrow();
        assertEquals("Bad Spell", fallback.get("name"));
        assertEquals("Failed to parse spell data", fallback.get("error"));

        Map<String, Object> good = results.stream()
                .filter(m -> "Good Spell".equals(m.get("name")))
                .findFirst()
                .orElseThrow();
        assertEquals("Good Spell", good.get("name"));
    }

    @Test
    void addSpellToCharacter_success() throws Exception {
        PlayerCharacter character = mock(PlayerCharacter.class);
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(userId);
        when(character.getOwner()).thenReturn(owner);

        Set<Spell> spells = new HashSet<>();
        when(character.getSpells()).thenReturn(spells);

        Spell spell = mock(Spell.class);
        when(spellRepository.findById(slug)).thenReturn(Optional.of(spell));
        when(spell.getSlug()).thenReturn(slug);
        when(spell.getName()).thenReturn("Fireball");
        when(spell.getRawJsonData()).thenReturn("{\"name\":\"Fireball\"}");
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(Map.of("name", "Fireball"));

        SpellSaveInputDTO input = new SpellSaveInputDTO(slug, name);

        CharacterSpellResponseDTO response = spellService.addSpellToCharacter(characterId, input, userId);

        assertNotNull(response);
        assertTrue(spells.contains(spell));
        verify(playerCharacterRepository).save(character);
    }

    @Test
    void addSpellToCharacter_characterNotFound_throws() {
        when(playerCharacterRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> spellService.addSpellToCharacter(characterId, new SpellSaveInputDTO(slug, name), userId));

        verify(spellRepository, never()).findById(anyString());
    }

    @Test
    void addSpellToCharacter_unauthorizedUser_throws() {
        PlayerCharacter character = mock(PlayerCharacter.class);
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(999L);
        when(character.getOwner()).thenReturn(owner);

        assertThrows(UnauthorizedAccessException.class,
                () -> spellService.addSpellToCharacter(characterId, new SpellSaveInputDTO(slug, name), userId));

        verify(playerCharacterRepository, never()).save(any());
    }

    @Test
    void addSpellToCharacter_spellNotInLocalDb_throwsNotFound() {
        PlayerCharacter character = mock(PlayerCharacter.class);
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(userId);
        when(character.getOwner()).thenReturn(owner);

        when(spellRepository.findById(slug)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> spellService.addSpellToCharacter(characterId, new SpellSaveInputDTO(slug, name), userId));

        verify(spellRepository).findById(slug);
        verify(playerCharacterRepository, never()).save(any());
    }

    @Test
    void addSpellToCharacter_jsonDeserializationError_throwsRuntimeException() throws Exception {
        PlayerCharacter character = mock(PlayerCharacter.class);
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(userId);
        when(character.getOwner()).thenReturn(owner);

        Set<Spell> spells = new HashSet<>();
        when(character.getSpells()).thenReturn(spells);

        Spell spell = mock(Spell.class);
        when(spellRepository.findById(slug)).thenReturn(Optional.of(spell));
        when(spell.getSlug()).thenReturn(slug);
        when(spell.getRawJsonData()).thenReturn("bad-json");

        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenThrow(new RuntimeException("Serialize failure"));

        SpellSaveInputDTO input = new SpellSaveInputDTO(slug, name);

        assertThrows(RuntimeException.class,
                () -> spellService.addSpellToCharacter(characterId, input, userId));

        assertTrue(spells.contains(spell));
        verify(playerCharacterRepository).save(character);
    }

    @Test
    void getSpellsForCharacter_success() throws Exception {
        PlayerCharacter character = mock(PlayerCharacter.class);
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));

        when(campaignPermissionService.canViewCharacter(character, userId)).thenReturn(true);

        Spell spell1 = mock(Spell.class);
        Spell spell2 = mock(Spell.class);
        when(spell1.getSlug()).thenReturn("fireball");
        when(spell1.getName()).thenReturn("Fireball");
        when(spell1.getRawJsonData()).thenReturn("json1");
        when(spell2.getSlug()).thenReturn("lightning");
        when(spell2.getName()).thenReturn("Lightning Bolt");
        when(spell2.getRawJsonData()).thenReturn("json2");

        Set<Spell> spells = new HashSet<>(Arrays.asList(spell1, spell2));
        when(character.getSpells()).thenReturn(spells);

        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(Map.of("name", "Spell"));

        List<CharacterSpellResponseDTO> result = spellService.getSpellsForCharacter(characterId, userId);

        assertEquals(2, result.size());
        verify(campaignPermissionService).canViewCharacter(character, userId);
        verify(playerCharacterRepository, never()).save(any());
    }

    @Test
    void getSpellsForCharacter_unauthorized_throws() {
        PlayerCharacter character = mock(PlayerCharacter.class);
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));

        when(campaignPermissionService.canViewCharacter(character, userId)).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class,
                () -> spellService.getSpellsForCharacter(characterId, userId));
    }

    @Test
    void getSpellsForCharacter_characterNotFound_throws() {
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> spellService.getSpellsForCharacter(characterId, userId));
    }

    @Test
    void getSpellsForCharacter_jsonDeserializationError_throwsRuntimeException() throws Exception {
        PlayerCharacter character = mock(PlayerCharacter.class);
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));

        when(campaignPermissionService.canViewCharacter(character, userId)).thenReturn(true);

        Spell spell = mock(Spell.class);
        when(spell.getSlug()).thenReturn(slug);
        when(spell.getRawJsonData()).thenReturn("bad-json");

        Set<Spell> spells = new HashSet<>();
        spells.add(spell);
        when(character.getSpells()).thenReturn(spells);

        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenThrow(new RuntimeException("Serialize failure"));

        assertThrows(RuntimeException.class,
                () -> spellService.getSpellsForCharacter(characterId, userId));

        verify(playerCharacterRepository, never()).save(any());
    }

    @Test
    void removeSpellFromCharacter_success() {
        PlayerCharacter character = mock(PlayerCharacter.class);
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(userId);
        when(character.getOwner()).thenReturn(owner);

        Spell spell = mock(Spell.class);
        when(spell.getSlug()).thenReturn(slug);
        Set<Spell> spells = new HashSet<>();
        spells.add(spell);
        when(character.getSpells()).thenReturn(spells);

        spellService.removeSpellFromCharacter(characterId, slug, userId);

        assertFalse(spells.contains(spell));
        verify(playerCharacterRepository).save(character);
    }

    @Test
    void removeSpellFromCharacter_spellNotOnCharacter_throws() {
        PlayerCharacter character = mock(PlayerCharacter.class);
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(userId);
        when(character.getOwner()).thenReturn(owner);

        when(character.getSpells()).thenReturn(new HashSet<>());

        assertThrows(ResourceNotFoundException.class,
                () -> spellService.removeSpellFromCharacter(characterId, slug, userId));

        verify(playerCharacterRepository, never()).save(any());
    }

    @Test
    void removeSpellFromCharacter_unauthorizedUser_throws() {
        PlayerCharacter character = mock(PlayerCharacter.class);
        when(playerCharacterRepository.findById(characterId)).thenReturn(Optional.of(character));

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(999L);
        when(character.getOwner()).thenReturn(owner);

        assertThrows(UnauthorizedAccessException.class,
                () -> spellService.removeSpellFromCharacter(characterId, slug, userId));

        verify(playerCharacterRepository, never()).save(any());
    }
}
