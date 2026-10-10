package org.kruskopf.backend.ship.controller;

import org.kruskopf.backend.ship.dto.ShipDTO;
import org.kruskopf.backend.ship.dto.ShipInputDTO;
import org.kruskopf.backend.ship.service.ShipService;
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

/**
 * Offworlders ship for a campaign. Any campaign participant may read and edit;
 * there is no delete endpoint (the ship is retained even if the campaign's
 * primary system changes away from OFFWORLDERS).
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/ship")
public class ShipController {

    private final ShipService shipService;

    public ShipController(ShipService shipService) {
        this.shipService = shipService;
    }

    @GetMapping
    public ResponseEntity<ShipDTO> getShip(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                           @PathVariable Long campaignId) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(shipService.getShip(campaignId, userId));
    }

    @PutMapping
    public ResponseEntity<ShipDTO> saveShip(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                            @PathVariable Long campaignId,
                                            @RequestBody ShipInputDTO dto) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(shipService.saveShip(campaignId, dto, userId));
    }

    /** Upload/replace the ship's profile image. */
    @PostMapping("/image")
    public ResponseEntity<ShipDTO> uploadImage(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                               @PathVariable Long campaignId,
                                               @RequestParam("file") MultipartFile file) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(shipService.uploadProfileImage(campaignId, file, userId));
    }

    /** Add an image to the ship's gallery. */
    @PostMapping("/images")
    public ResponseEntity<ShipDTO> addGalleryImage(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                   @PathVariable Long campaignId,
                                                   @RequestParam("file") MultipartFile file) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(shipService.addGalleryImage(campaignId, file, userId));
    }

    /** Remove an image from the ship's gallery by its stored URL. */
    @DeleteMapping("/images")
    public ResponseEntity<ShipDTO> removeGalleryImage(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                      @PathVariable Long campaignId,
                                                      @RequestParam("url") String url) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(shipService.removeGalleryImage(campaignId, url, userId));
    }
}
