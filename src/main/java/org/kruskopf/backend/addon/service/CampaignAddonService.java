package org.kruskopf.backend.addon.service;

import org.kruskopf.backend.addon.AddonKey;
import org.kruskopf.backend.addon.entity.CampaignAddon;
import org.kruskopf.backend.addon.repository.CampaignAddonRepository;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.InvalidRequestException;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The campaign add-on switch: which optional sections a campaign has.
 * <p>
 * Reading which add-ons are on is part of the campaign every participant already
 * receives; enabling and disabling is administration, so it sits on the ownership
 * axis beside roster and roles and only the campaign owner may do it.
 * <p>
 * Enabling is idempotent and disabling is a delete: the row's presence is the
 * state, so a double click cannot produce a duplicate (the unique key on
 * {@code (campaign_id, addon_key)} backs that up) and there is no flag to drift.
 * An unknown key is refused rather than stored, which is what keeps
 * {@link AddonKey} the registry.
 */
@Service
public class CampaignAddonService {

    private final CampaignAddonRepository addonRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignPermissionService campaignPermissionService;

    public CampaignAddonService(CampaignAddonRepository addonRepository,
                                CampaignRepository campaignRepository,
                                CampaignPermissionService campaignPermissionService) {
        this.addonRepository = addonRepository;
        this.campaignRepository = campaignRepository;
        this.campaignPermissionService = campaignPermissionService;
    }

    /**
     * The add-on keys enabled on one campaign.
     *
     * @param campaignId the campaign
     * @return the enabled keys as the API spells them (`planner`); empty for none
     */
    @Transactional(readOnly = true)
    public List<String> enabledAddonKeys(Long campaignId) {
        return readEnabledAddonKeys(campaignId);
    }

    /**
     * The enabled keys of many campaigns at once, for the campaign list endpoints
     * — one query, whatever the number of campaigns.
     *
     * @param campaignIds the campaigns to look up
     * @return campaign id → its enabled keys; a campaign with none is absent
     */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> enabledAddonKeysFor(Collection<Long> campaignIds) {
        return readEnabledAddonKeysFor(campaignIds);
    }

    /** Whether one add-on is on for a campaign. Used to gate its own endpoints. */
    @Transactional(readOnly = true)
    public boolean isEnabled(Long campaignId, AddonKey addonKey) {
        return addonRepository.findByCampaignIdAndAddonKey(campaignId, addonKey).isPresent();
    }

    /**
     * Switch an add-on on. Idempotent: enabling what is already on changes
     * nothing and is not an error.
     *
     * @param campaignId the campaign
     * @param rawKey     the add-on key as it arrived in the path
     * @param userId     the caller, who must be the campaign owner
     * @return the campaign's enabled keys after the change
     */
    @Transactional
    public List<String> enable(Long campaignId, String rawKey, long userId) {
        AddonKey addonKey = requireKnownKey(rawKey);
        requireOwner(campaignId, userId);

        if (addonRepository.findByCampaignIdAndAddonKey(campaignId, addonKey).isEmpty()) {
            addonRepository.saveAndFlush(new CampaignAddon(requireCampaign(campaignId), addonKey));
        }

        return readEnabledAddonKeys(campaignId);
    }

    /**
     * Switch an add-on off. Also idempotent — disabling what is off is a no-op.
     * The add-on's own data is deliberately left alone: switching the planner off
     * and on again returns the plans, exactly as removing and re-adding a
     * participant returns their journal.
     *
     * @param campaignId the campaign
     * @param rawKey     the add-on key as it arrived in the path
     * @param userId     the caller, who must be the campaign owner
     * @return the campaign's enabled keys after the change
     */
    @Transactional
    public List<String> disable(Long campaignId, String rawKey, long userId) {
        AddonKey addonKey = requireKnownKey(rawKey);
        requireOwner(campaignId, userId);

        Optional<CampaignAddon> existing = addonRepository.findByCampaignIdAndAddonKey(campaignId, addonKey);
        existing.ifPresent(addonRepository::delete);

        return readEnabledAddonKeys(campaignId);
    }

    /**
     * The read behind the public entry points, kept non-transactional on purpose:
     * called from within a write transaction it simply joins that transaction, and
     * called from an entry point it runs under that entry point's read-only one.
     * A private helper carries no {@code @Transactional}, so no self-invocation of
     * an annotated method can occur.
     */
    private List<String> readEnabledAddonKeys(Long campaignId) {
        return readEnabledAddonKeysFor(List.of(campaignId))
                .getOrDefault(campaignId, List.of());
    }

    private Map<Long, List<String>> readEnabledAddonKeysFor(Collection<Long> campaignIds) {
        if (campaignIds == null || campaignIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<String>> byCampaign = new HashMap<>();
        for (CampaignAddon addon : addonRepository.findByCampaignIdIn(campaignIds)) {
            byCampaign
                    .computeIfAbsent(addon.getCampaign().getId(), id -> new ArrayList<>())
                    .add(addon.getAddonKey().key());
        }
        byCampaign.values().forEach(keys -> keys.sort(String::compareTo));
        return byCampaign;
    }

    private AddonKey requireKnownKey(String rawKey) {
        return AddonKey.fromKey(rawKey)
                .orElseThrow(() -> new InvalidRequestException("Unknown add-on: " + rawKey));
    }

    private void requireOwner(Long campaignId, long userId) {
        requireCampaign(campaignId);
        if (!campaignPermissionService.isCampaignOwner(campaignId, userId)) {
            throw new UnauthorizedAccessException("Only the campaign owner can manage add-ons");
        }
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + campaignId));
    }
}
