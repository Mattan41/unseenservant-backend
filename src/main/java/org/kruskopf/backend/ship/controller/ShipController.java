package org.kruskopf.backend.ship.controller;

import org.kruskopf.backend.ship.dto.ShipDTO;
import org.kruskopf.backend.ship.dto.ShipInputDTO;
import org.kruskopf.backend.ship.service.ShipService;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
