package org.kruskopf.backend.ship.service;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.entity.CampaignRole;
import org.kruskopf.backend.campaign.entity.CampaignUser;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.repository.CampaignUserRepository;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.filestorage.StoredFileTransaction;
import org.kruskopf.backend.ship.ShipCrewCharacter;
import org.kruskopf.backend.ship.dto.ShipCrewInputDTO;
import org.kruskopf.backend.ship.dto.ShipCrewMemberDTO;
import org.kruskopf.backend.ship.entity.Ship;
import org.kruskopf.backend.ship.entity.ShipCrewMember;
import org.kruskopf.backend.ship.repository.ShipCrewRepository;
import org.kruskopf.backend.ship.repository.ShipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ship crew manifest: one entry per participant, each holding free-text notes,
 * gallery images and the characters the participant has aboard.
 * <p>
 * Entries are keyed on the user, so a participant who leaves the campaign keeps
 * their entry (it is simply no longer returned); rejoining re-activates it. Any
 * participant may read the manifest; each participant may edit only their own
 * entry.
 */
@Service
public class ShipCrewService {

    private final ShipRepository shipRepository;
    private final ShipCrewRepository shipCrewRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignUserRepository campaignUserRepository;
    private final StoredFileTransaction storedFileTransaction;

    public ShipCrewService(ShipRepository shipRepository,
                           ShipCrewRepository shipCrewRepository,
                           CampaignRepository campaignRepository,
                           CampaignUserRepository campaignUserRepository,
                           StoredFileTransaction storedFileTransaction) {
        this.shipRepository = shipRepository;
        this.shipCrewRepository = shipCrewRepository;
        this.campaignRepository = campaignRepository;
        this.campaignUserRepository = campaignUserRepository;
        this.storedFileTransaction = storedFileTransaction;
    }

    /**
     * The crew manifest for the campaign's current participants, in roster
     * order. A participant without an entry yet is returned with empty fields so
     * the UI always has one card each. Participants who have left are omitted —
     * their entries are retained, not deleted.
     */
    @Transactional(readOnly = true)
    public List<ShipCrewMemberDTO> getCrew(Long campaignId, long viewerId) {
        requireCampaign(campaignId);
        requireParticipant(campaignId, viewerId);

        List<CampaignUser> participants = campaignUserRepository.findByCampaignId(campaignId);
        Ship ship = shipRepository.findByCampaignId(campaignId).orElse(null);
        Map<Long, ShipCrewMember> byUser = ship == null
                ? Map.of()
                : shipCrewRepository.findByShipId(ship.getId()).stream()
                        .collect(Collectors.toMap(
                                member -> member.getUser().getId(),
                                Function.identity(),
                                (first, second) -> first));

        return participants.stream()
                .map(participant -> toDTO(participant, byUser.get(participant.getUser().getId())))
                .toList();
    }

    /** Create or update the caller's own crew entry. */
    @Transactional
    public ShipCrewMemberDTO saveMyEntry(Long campaignId, ShipCrewInputDTO dto, long userId) {
        CampaignUser participant = requireParticipant(campaignId, userId);
        ShipCrewMember member = loadMember(campaignId, participant);

        if (dto.notes() != null) {
            member.setNotes(dto.notes());
        }
        if (dto.characters() != null) {
            member.setCharacters(new ArrayList<>(dto.characters()));
        }

        return toDTO(participant, shipCrewRepository.save(member));
    }

    /**
     * Add a gallery image to the caller's own entry.
     * <p>
     * Deliberately not {@code @Transactional}: the file is written before the
     * transaction opens so no database connection is held during the write, and
     * {@link StoredFileTransaction} removes it again if the transaction fails. The
     * participant check has to come first for the same reason — a refused request
     * must not touch the disk.
     */
    public ShipCrewMemberDTO addMyImage(Long campaignId, MultipartFile file, long userId) {
        CampaignUser participant = requireParticipant(campaignId, userId);

        return storedFileTransaction.storeThenRun(
                file, "ship_crew_" + campaignId + "_" + userId, "IMAGE", storedName -> {
                    ShipCrewMember member = loadMember(campaignId, participant);
                    List<String> urls = new ArrayList<>(member.getImageUrls());
                    urls.add("/images/" + storedName);
                    member.setImageUrls(urls);
                    return toDTO(participant, shipCrewRepository.save(member));
                });
    }

    /** Remove a gallery image from the caller's own entry by its stored URL. */
    @Transactional
    public ShipCrewMemberDTO removeMyImage(Long campaignId, String url, long userId) {
        CampaignUser participant = requireParticipant(campaignId, userId);
        ShipCrewMember member = loadMember(campaignId, participant);
        List<String> urls = new ArrayList<>(member.getImageUrls());
        urls.remove(url);
        member.setImageUrls(urls);
        return toDTO(participant, shipCrewRepository.save(member));
    }

    // --- helpers ---

    private ShipCrewMember loadMember(Long campaignId, CampaignUser participant) {
        Campaign campaign = requireCampaign(campaignId);
        Ship ship = shipRepository.findByCampaignId(campaignId)
                .orElseGet(() -> createDefaultShip(campaign));
        return shipCrewRepository.findByShipIdAndUserId(ship.getId(), participant.getUser().getId())
                .orElseGet(() -> new ShipCrewMember(ship, participant.getUser()));
    }

    private ShipCrewMemberDTO toDTO(CampaignUser participant, ShipCrewMember member) {
        List<String> imageUrls = member != null && member.getImageUrls() != null
                ? List.copyOf(member.getImageUrls())
                : List.of();
        List<ShipCrewCharacter> characters = member != null && member.getCharacters() != null
                ? List.copyOf(member.getCharacters())
                : List.of();
        CampaignRole role = participant.getRole();
        return new ShipCrewMemberDTO(
                participant.getUser().getId(),
                participant.getNickname(),
                role != null ? role.name() : CampaignRole.PLAYER.name(),
                member != null ? member.getNotes() : "",
                imageUrls,
                characters);
    }

    private Campaign requireCampaign(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign not found with id: " + campaignId));
    }

    private CampaignUser requireParticipant(Long campaignId, long userId) {
        return campaignUserRepository.findByCampaignId(campaignId).stream()
                .filter(participant -> participant.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new UnauthorizedAccessException(
                        "User is not a participant in this campaign"));
    }

    private Ship createDefaultShip(Campaign campaign) {
        Ship ship = Ship.createDefault();
        ship.setCampaign(campaign);
        return shipRepository.save(ship);
    }
}
