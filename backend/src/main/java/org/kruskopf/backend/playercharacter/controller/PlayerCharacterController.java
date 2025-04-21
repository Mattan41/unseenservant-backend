package org.kruskopf.backend.playercharacter.controller;

import jakarta.validation.Valid;
import org.kruskopf.backend.playercharacter.dto.PlayerCharacterCampaignUpdateDTO;
import org.kruskopf.backend.playercharacter.dto.PlayerCharacterInputDTO;
import org.kruskopf.backend.playercharacter.dto.PlayerCharacterOutputDTO;
import org.kruskopf.backend.playercharacter.service.PlayerCharacterService;
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
public class PlayerCharacterController {

    private final PlayerCharacterService playerCharacterService;

    public PlayerCharacterController(PlayerCharacterService playerCharacterService) {
        this.playerCharacterService = playerCharacterService;
    }

    // add preauthorize to all endpoints

    @PostMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<PlayerCharacterOutputDTO> createCharacter(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody PlayerCharacterInputDTO inputDTO) {

        Long userId = customUserDetails.user().getId();

        PlayerCharacterInputDTO effectiveDTO = inputDTO;
        if (inputDTO.ownerId() == null) {
            effectiveDTO = new PlayerCharacterInputDTO(
                    userId,
                    null,
                    inputDTO.name(),
                    inputDTO.level(),
                    inputDTO.characterClass(),
                    inputDTO.imageUrl(),
                    inputDTO.race(),
                    inputDTO.playerCharacterData()
            );
        }

        PlayerCharacterOutputDTO createdCharacter = playerCharacterService.createCharacter(effectiveDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCharacter);
    }

    @GetMapping
    public ResponseEntity<List<PlayerCharacterOutputDTO>> getCharactersByCampaignId(
            @RequestParam(required = false) Long campaignId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.user().getId();

        List<PlayerCharacterOutputDTO> characters = playerCharacterService.getCharactersByCampaignId(campaignId, userId);

        return ResponseEntity.ok(characters);
    }

    // get a list of all characters without campaign id for a user
    @GetMapping("/without-campaign")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<PlayerCharacterOutputDTO>> getCharactersWithoutCampaign(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.user().getId();
        List<PlayerCharacterOutputDTO> characters = playerCharacterService.getCharactersWithoutCampaign(userId);
        return ResponseEntity.ok(characters);
    }


    @GetMapping("/me")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<PlayerCharacterOutputDTO>> getMyCharacters(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        List<PlayerCharacterOutputDTO> characters = playerCharacterService.getCharactersByUserId(userId);
        return ResponseEntity.ok(characters);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerCharacterOutputDTO> getCharacterById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(playerCharacterService.getCharacterById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{characterId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<PlayerCharacterOutputDTO> updateCharacter(
            @PathVariable Long characterId,
            @Valid @RequestBody PlayerCharacterInputDTO inputDTO,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.user().getId();
        PlayerCharacterOutputDTO updatedCharacter = playerCharacterService.updateCharacter(characterId, inputDTO, userId);
        return ResponseEntity.ok(updatedCharacter);
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<PlayerCharacterOutputDTO> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();

        PlayerCharacterOutputDTO updatedImage = playerCharacterService.uploadCharacterImage(id, file, userId);

        return ResponseEntity.ok(updatedImage);
    }

    @PatchMapping("/{characterId}/campaign")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<PlayerCharacterOutputDTO> addCharacterToCampaign(
            @PathVariable Long characterId,
            @RequestBody PlayerCharacterCampaignUpdateDTO updateDTO,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.user().getId();
        PlayerCharacterOutputDTO updatedCharacter = playerCharacterService.addCharacterToCampaign(characterId, updateDTO.campaignId(), userId);
        return ResponseEntity.ok(updatedCharacter);
    }

    @DeleteMapping("/{characterId}/campaign")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<PlayerCharacterOutputDTO> removeCharacterFromCampaign(
            @PathVariable Long characterId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        Long userId = customUserDetails.user().getId();
        PlayerCharacterOutputDTO updatedCharacter = playerCharacterService.removeCharacterFromCampaign(characterId, userId);
        return ResponseEntity.ok(updatedCharacter);
    }

    @DeleteMapping("/{characterId}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Void> deleteCharacter(@PathVariable Long characterId, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        playerCharacterService.deleteCharacter(characterId, userId);
        return ResponseEntity.noContent().build();
    }


}
