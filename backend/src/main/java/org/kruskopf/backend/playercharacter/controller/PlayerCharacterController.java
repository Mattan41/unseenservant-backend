package org.kruskopf.backend.playercharacter.controller;

import org.kruskopf.backend.playercharacter.dto.PlayerCharacterInputDTO;
import org.kruskopf.backend.playercharacter.dto.PlayerCharacterOutputDTO;
import org.kruskopf.backend.playercharacter.service.PlayerCharacterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
public class PlayerCharacterController {

    private final PlayerCharacterService playerCharacterService;

    public PlayerCharacterController(PlayerCharacterService playerCharacterService) {
        this.playerCharacterService = playerCharacterService;
    }

    @GetMapping
    public List<PlayerCharacterOutputDTO> getAllCharacters() {
        return playerCharacterService.getAllCharacters();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerCharacterOutputDTO> getCharacterById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(playerCharacterService.getCharacterById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<PlayerCharacterOutputDTO> createCharacter(@RequestBody PlayerCharacterInputDTO inputDTO) {
        return ResponseEntity.ok(playerCharacterService.createCharacter(inputDTO));
    }
}
