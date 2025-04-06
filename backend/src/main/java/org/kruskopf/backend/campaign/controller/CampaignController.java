package org.kruskopf.backend.campaign.controller;

import org.kruskopf.backend.campaign.dto.CampaignCreationDTO;
import org.kruskopf.backend.campaign.dto.CampaignResponseDTO;
import org.kruskopf.backend.campaign.dto.CampaignUpdateDTO;
import org.kruskopf.backend.campaign.dto.UpdateParticipantsDTO;
import org.kruskopf.backend.campaign.service.CampaignService;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ResponseEntity<CampaignResponseDTO> createCampaign(@AuthenticationPrincipal CustomUserDetails customUserDetails, @RequestBody CampaignCreationDTO campaignDTO) {
        Long userId = customUserDetails.user().getId();

        CampaignCreationDTO effectiveDTO = campaignDTO;
        if (campaignDTO.ownerId() == null) {
            effectiveDTO = new CampaignCreationDTO(
                    campaignDTO.name(),
                    campaignDTO.description(),
                    userId,
                    campaignDTO.participants()
            );
        }

        CampaignResponseDTO createdCampaign = campaignService.createCampaign(effectiveDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCampaign);
    }

    @GetMapping
    public ResponseEntity<List<CampaignResponseDTO>> getAllCampaigns() {
        List<CampaignResponseDTO> campaigns = campaignService.getAllCampaigns();
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<List<CampaignResponseDTO>> getAllCampaignsForCurrentUser(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        List<CampaignResponseDTO> campaigns = campaignService.getAllCampaignsForCurrentUser(userId);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponseDTO> getCampaignById(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long id) {
        Long userId = customUserDetails.user().getId();
        CampaignResponseDTO campaignResponse = campaignService.getCampaignByIdIfAuthorized(id, userId);
        return ResponseEntity.ok(campaignResponse);
    }


    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponseDTO> updateCampaign(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long id,
            @RequestBody CampaignUpdateDTO campaignDTO) {
        Long userId = customUserDetails.user().getId();
        CampaignResponseDTO updatedCampaign = campaignService.updateCampaign(id, campaignDTO, userId);
        return ResponseEntity.ok(updatedCampaign);
    }

    // participants management
    @PatchMapping("/{id}/participants")
    public ResponseEntity<CampaignResponseDTO> updateParticipants(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long id,
            @RequestBody UpdateParticipantsDTO updateDTO) {
        Long userId = customUserDetails.user().getId();
        CampaignResponseDTO updatedCampaign = campaignService.updateParticipants(id, updateDTO, userId);
        return ResponseEntity.ok(updatedCampaign);
    }

    @PatchMapping("/{id}/participants/{participantId}/nickname")
    public ResponseEntity<CampaignResponseDTO> updateParticipantNickname(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long id,
            @PathVariable Long participantId,
            @RequestBody String nickname) {
        Long userId = customUserDetails.user().getId();
        CampaignResponseDTO updatedCampaign = campaignService.updateParticipantNickname(id, participantId, nickname, userId);
        return ResponseEntity.ok(updatedCampaign);
    }

    @PatchMapping("/{id}/participants/{participantId}/role")
    public ResponseEntity<CampaignResponseDTO> updateParticipantRole(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long id,
            @PathVariable Long participantId,
            @RequestBody String role) {
        Long userId = customUserDetails.user().getId();
        CampaignResponseDTO updatedCampaign = campaignService.updateParticipantRole(id, participantId, role, userId);
        return ResponseEntity.ok(updatedCampaign);
    }


    //campaign management
    @PatchMapping("/{id}/owner")
    public ResponseEntity<CampaignResponseDTO> transferOwnership(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long id,
            @RequestParam Long newOwnerId) {
        Long currentUserId = customUserDetails.user().getId();
        CampaignResponseDTO updatedCampaign = campaignService.transferOwnership(id, newOwnerId, currentUserId);
        return ResponseEntity.ok(updatedCampaign);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long id) {
        Long userId = customUserDetails.user().getId();
        campaignService.deleteCampaign(id, userId);
        return ResponseEntity.noContent().build();
    }
}
