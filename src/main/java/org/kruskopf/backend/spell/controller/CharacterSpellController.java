package org.kruskopf.backend.spell.controller;

import org.kruskopf.backend.spell.dto.CharacterSpellResponseDTO;
import org.kruskopf.backend.spell.dto.SpellSaveInputDTO;
import org.kruskopf.backend.spell.service.SpellService;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
public class CharacterSpellController {

    private final SpellService spellService;

    public CharacterSpellController(SpellService spellService) {
        this.spellService = spellService;
    }

    @PostMapping("/{characterId}/spells")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CharacterSpellResponseDTO> addSpell(
            @PathVariable Long characterId,
            @RequestBody SpellSaveInputDTO inputDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.user().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spellService.addSpellToCharacter(characterId, inputDTO, userId));
    }

    @GetMapping("/{characterId}/spells")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<CharacterSpellResponseDTO>> getSpells(
            @PathVariable Long characterId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.user().getId();
        return ResponseEntity.ok(spellService.getSpellsForCharacter(characterId, userId));
    }

    @DeleteMapping("/{characterId}/spells/{slug}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Void> removeSpell(
            @PathVariable Long characterId,
            @PathVariable String slug,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.user().getId();
        spellService.removeSpellFromCharacter(characterId, slug, userId);
        return ResponseEntity.noContent().build();
    }
}
