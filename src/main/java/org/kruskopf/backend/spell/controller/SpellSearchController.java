package org.kruskopf.backend.spell.controller;

import org.kruskopf.backend.spell.service.SpellService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spells")
public class SpellSearchController {

    private final SpellService spellService;

    public SpellSearchController(SpellService spellService) {
        this.spellService = spellService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> searchSpells(@RequestParam(defaultValue = "") String query) {
        return ResponseEntity.ok(spellService.searchSpells(query));
    }
}
