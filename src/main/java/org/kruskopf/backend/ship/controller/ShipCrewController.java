package org.kruskopf.backend.ship.controller;

import org.kruskopf.backend.ship.dto.ShipCrewInputDTO;
import org.kruskopf.backend.ship.dto.ShipCrewMemberDTO;
import org.kruskopf.backend.ship.service.ShipCrewService;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Ship crew manifest for a campaign. Any participant may read the manifest; each
 * participant may edit only their own entry ({@code /me}). Entries are retained
 * when a participant leaves and re-activate when they rejoin.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/ship/crew")
public class ShipCrewController {

    private final ShipCrewService shipCrewService;

    public ShipCrewController(ShipCrewService shipCrewService) {
        this.shipCrewService = shipCrewService;
    }

    @GetMapping
    public ResponseEntity<List<ShipCrewMemberDTO>> getCrew(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long campaignId) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(shipCrewService.getCrew(campaignId, userId));
    }

    /** Create/update the caller's own entry (notes + characters). */
    @PutMapping("/me")
    public ResponseEntity<ShipCrewMemberDTO> saveMyEntry(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long campaignId,
            @RequestBody ShipCrewInputDTO dto) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(shipCrewService.saveMyEntry(campaignId, dto, userId));
    }

    /** Add an image to the caller's own entry (shown in the ship's gallery). */
    @PostMapping("/me/images")
    public ResponseEntity<ShipCrewMemberDTO> addMyImage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long campaignId,
            @RequestParam("file") MultipartFile file) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(shipCrewService.addMyImage(campaignId, file, userId));
    }

    /** Remove an image from the caller's own entry by its stored URL. */
    @DeleteMapping("/me/images")
    public ResponseEntity<ShipCrewMemberDTO> removeMyImage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long campaignId,
            @RequestParam("url") String url) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(shipCrewService.removeMyImage(campaignId, url, userId));
    }
}
