package org.kruskopf.backend.addon.controller;

import org.kruskopf.backend.addon.service.CampaignAddonService;
import org.kruskopf.backend.campaign.dto.CampaignResponseDTO;
import org.kruskopf.backend.campaign.service.CampaignService;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The optional add-ons of a campaign.
 * <p>
 * A second controller on the {@code /api/campaigns} prefix on purpose: the add-on
 * switch is its own concern with its own keys, and keeping it here means
 * {@code CampaignController} keeps growing no further. The paths do not overlap.
 * <p>
 * Every write answers with the whole campaign, so the client has the new
 * {@code addons} list in hand and the navigation can be rebuilt from one
 * response. Only the campaign owner may change them
 * ({@link CampaignAddonService}); reading them needs nothing extra, since the
 * campaign carries them for every participant anyway.
 */
@RestController
@RequestMapping("/api/campaigns")
@PreAuthorize("hasRole('ROLE_USER')")
public class CampaignAddonController {

    private final CampaignAddonService campaignAddonService;
    private final CampaignService campaignService;

    public CampaignAddonController(CampaignAddonService campaignAddonService,
                                   CampaignService campaignService) {
        this.campaignAddonService = campaignAddonService;
        this.campaignService = campaignService;
    }

    @GetMapping("/{id}/addons")
    public ResponseEntity<List<String>> getAddons(@PathVariable Long id,
                                                 @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        // Same visibility rule as the campaign itself: a participant, or a 403.
        campaignService.getCampaignByIdIfAuthorized(id, userId);
        return ResponseEntity.ok(campaignAddonService.enabledAddonKeys(id));
    }

    @PostMapping("/{id}/addons/{addonKey}")
    public ResponseEntity<CampaignResponseDTO> enableAddon(@PathVariable Long id,
                                                           @PathVariable String addonKey,
                                                           @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        campaignAddonService.enable(id, addonKey, userId);
        return ResponseEntity.ok(campaignService.getCampaignByIdIfAuthorized(id, userId));
    }

    @DeleteMapping("/{id}/addons/{addonKey}")
    public ResponseEntity<CampaignResponseDTO> disableAddon(@PathVariable Long id,
                                                            @PathVariable String addonKey,
                                                            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        campaignAddonService.disable(id, addonKey, userId);
        return ResponseEntity.ok(campaignService.getCampaignByIdIfAuthorized(id, userId));
    }
}
