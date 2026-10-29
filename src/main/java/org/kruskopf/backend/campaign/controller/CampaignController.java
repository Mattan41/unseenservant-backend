package org.kruskopf.backend.campaign.controller;

import org.kruskopf.backend.campaign.dto.*;
import org.kruskopf.backend.campaign.service.CampaignActivityService;
import org.kruskopf.backend.campaign.service.CampaignParticipantService;
import org.kruskopf.backend.campaign.service.CampaignService;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
// Every campaign endpoint is role-shaped at this level; the resource-shaped rules
// (owner / GM / participant) live in CampaignService and CampaignParticipantService,
// both of them behind CampaignPermissionService.
@PreAuthorize("hasRole('ROLE_USER')")
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignParticipantService campaignParticipantService;
    private final CampaignActivityService campaignActivityService;

    public CampaignController(CampaignService campaignService,
                              CampaignParticipantService campaignParticipantService,
                              CampaignActivityService campaignActivityService) {
        this.campaignService = campaignService;
        this.campaignParticipantService = campaignParticipantService;
        this.campaignActivityService = campaignActivityService;
    }

    @PostMapping
    public ResponseEntity<CampaignResponseDTO> createCampaign(@AuthenticationPrincipal CustomUserDetails customUserDetails, @RequestBody CampaignCreationDTO campaignDTO) {
        Long userId = customUserDetails.user().getId();

        CampaignCreationDTO effectiveDTO = campaignDTO;
        if (campaignDTO.ownerId() == null) {
            effectiveDTO = new CampaignCreationDTO(
                    campaignDTO.name(),
                    campaignDTO.description(),
                    campaignDTO.worldLore(),
                    campaignDTO.privateDescription(),
                    userId,
                    campaignDTO.participants(),
                    campaignDTO.primarySystem()
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
    public ResponseEntity<List<CampaignResponseDTO>> getAllCampaignsForCurrentUser(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        List<CampaignResponseDTO> campaigns = campaignService.getAllCampaignsForCurrentUser(userId);
        return ResponseEntity.ok(campaigns);
    }

    /**
     * What has changed in each of the caller's campaigns: the timestamps the
     * client badges its navigation with. Deliberately one request for the whole
     * list, and deliberately scoped to the campaigns {@code GET /api/campaigns/me}
     * returns (see {@link CampaignActivityService}).
     * <p>
     * A literal path, so it is matched ahead of {@code /{id}} — "activity" is not
     * a campaign id and must never be read as one.
     */
    @GetMapping("/activity")
    public ResponseEntity<List<CampaignActivityDTO>> getCampaignActivity(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(campaignActivityService.getActivityForCurrentUser(userId));
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

    @PostMapping("/{id}/image")
    public ResponseEntity<CampaignResponseDTO> uploadCampaignImage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Long userId = customUserDetails.user().getId();
        CampaignResponseDTO updatedCampaign = campaignService.uploadCampaignImage(id, file, userId);
        return ResponseEntity.ok(updatedCampaign);
    }


    // participants management
    @PatchMapping("/{id}/participants")
    public ResponseEntity<CampaignResponseDTO> updateParticipants(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long id,
            @RequestBody UpdateParticipantsDTO updateDTO) {
        Long userId = customUserDetails.user().getId();
        CampaignResponseDTO updatedCampaign = campaignParticipantService.updateParticipants(id, updateDTO, userId);
        return ResponseEntity.ok(updatedCampaign);
    }

    @PatchMapping("/{id}/participants/{participantId}/nickname")
    public ResponseEntity<CampaignResponseDTO> updateParticipantNickname(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long id,
            @PathVariable Long participantId,
            @RequestBody String nickname) {
        Long userId = customUserDetails.user().getId();
        CampaignResponseDTO updatedCampaign = campaignParticipantService.updateParticipantNickname(id, participantId, nickname, userId);
        return ResponseEntity.ok(updatedCampaign);
    }

    @PatchMapping("/{id}/participants/{participantId}/role")
    public ResponseEntity<CampaignResponseDTO> updateParticipantRole(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long id,
            @PathVariable Long participantId,
            @RequestBody String role) {
        Long userId = customUserDetails.user().getId();
        CampaignResponseDTO updatedCampaign = campaignParticipantService.updateParticipantRole(id, participantId, role, userId);
        return ResponseEntity.ok(updatedCampaign);
    }


    @PatchMapping("/{id}/owner")
    public ResponseEntity<CampaignResponseDTO> transferOwnership(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long id,
            @RequestBody CampaignTransferOwnerDTO request) {
        Long currentUserId = customUserDetails.user().getId();
        CampaignResponseDTO updatedCampaign = campaignParticipantService.transferOwnership(id, request.newOwnerId(), currentUserId);
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
