package org.kruskopf.backend.playercharacter.controller;

import jakarta.validation.Valid;
import org.kruskopf.backend.playercharacter.dto.CharacterCampaignUpdateDTO;
import org.kruskopf.backend.playercharacter.dto.CharacterInputDTO;
import org.kruskopf.backend.playercharacter.dto.CharacterOutputDTO;
import org.kruskopf.backend.playercharacter.service.GameCharacterService;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
public class GameCharacterController {

    private final GameCharacterService gameCharacterService;

    public GameCharacterController(GameCharacterService gameCharacterService) {
        this.gameCharacterService = gameCharacterService;
    }

    // add preauthorize to all endpoints

    @PostMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CharacterOutputDTO> createCharacter(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody CharacterInputDTO inputDTO) {

        Long userId = customUserDetails.user().getId();

        CharacterInputDTO effectiveDTO = inputDTO;
        if (inputDTO.ownerId() == null) {
            effectiveDTO = new CharacterInputDTO(
                    userId,
                    null,
                    inputDTO.name(),
                    inputDTO.systemType(),
                    inputDTO.notes(),
                    inputDTO.avatarUrl(),
                    inputDTO.dnd5e(),
                    inputDTO.offworlders()
            );
        }

        CharacterOutputDTO createdCharacter = gameCharacterService.createCharacter(effectiveDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCharacter);
    }

    @GetMapping
    public ResponseEntity<List<CharacterOutputDTO>> getCharactersByCampaignId(
            @RequestParam(required = false) Long campaignId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.user().getId();

        List<CharacterOutputDTO> characters = gameCharacterService.getCharactersByCampaignId(campaignId, userId);

        return ResponseEntity.ok(characters);
    }

    // get a list of all characters without campaign id for a user
    @GetMapping("/without-campaign")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<CharacterOutputDTO>> getCharactersWithoutCampaign(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.user().getId();
        List<CharacterOutputDTO> characters = gameCharacterService.getCharactersWithoutCampaign(userId);
        return ResponseEntity.ok(characters);
    }


    @GetMapping("/me")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<CharacterOutputDTO>> getMyCharacters(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        List<CharacterOutputDTO> characters = gameCharacterService.getCharactersByUserId(userId);
        return ResponseEntity.ok(characters);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CharacterOutputDTO> getCharacterById(@PathVariable Long id,
                                                               @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        try {
            return ResponseEntity.ok(gameCharacterService.getCharacterById(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{characterId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CharacterOutputDTO> updateCharacter(
            @PathVariable Long characterId,
            @Valid @RequestBody CharacterInputDTO inputDTO,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.user().getId();
        CharacterOutputDTO updatedCharacter = gameCharacterService.updateCharacter(characterId, inputDTO, userId);
        return ResponseEntity.ok(updatedCharacter);
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CharacterOutputDTO> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();

        CharacterOutputDTO updatedImage = gameCharacterService.uploadCharacterImage(id, file, userId);

        return ResponseEntity.ok(updatedImage);
    }

    @PatchMapping("/{characterId}/campaign")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CharacterOutputDTO> addCharacterToCampaign(
            @PathVariable Long characterId,
            @RequestBody CharacterCampaignUpdateDTO updateDTO,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.user().getId();
        CharacterOutputDTO updatedCharacter = gameCharacterService.addCharacterToCampaign(characterId, updateDTO.campaignId(), userId);
        return ResponseEntity.ok(updatedCharacter);
    }

    @DeleteMapping("/{characterId}/campaign")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<CharacterOutputDTO> removeCharacterFromCampaign(
            @PathVariable Long characterId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.user().getId();
        CharacterOutputDTO updatedCharacter = gameCharacterService.removeCharacterFromCampaign(characterId, userId);
        return ResponseEntity.ok(updatedCharacter);
    }

    @DeleteMapping("/{characterId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Void> deleteCharacter(@PathVariable Long characterId, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        gameCharacterService.deleteCharacter(characterId, userId);
        return ResponseEntity.noContent().build();
    }


}
