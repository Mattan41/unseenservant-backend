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
        return ResponseEntity.ok(campaignService.getCampaignById(id));
    }

    @PostMapping
    public ResponseEntity<CampaignResponseDTO> createCampaign(@AuthenticationPrincipal CustomUserDetails customUserDetails, @RequestBody CampaignCreationDTO campaignDTO) {
        Long userId = customUserDetails.user().getId();
        CampaignResponseDTO createdCampaign = campaignService.createCampaign(campaignDTO, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCampaign);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponseDTO> updateCampaign(
            @PathVariable Long id,
            @RequestBody CampaignUpdateDTO campaignDTO) {
        CampaignResponseDTO updatedCampaign = campaignService.updateCampaign(id, campaignDTO);
        return ResponseEntity.ok(updatedCampaign);
    }

    @PatchMapping("/{id}/participants")
    public ResponseEntity<CampaignResponseDTO> updateParticipants(
            @PathVariable Long id,
            @RequestBody UpdateParticipantsDTO updateDTO) {
        CampaignResponseDTO updatedCampaign = campaignService.updateParticipants(id, updateDTO);
        return ResponseEntity.ok(updatedCampaign);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }
}
