package org.kruskopf.backend.campaign.service;

import org.kruskopf.backend.campaign.dto.CampaignDTO;
import org.kruskopf.backend.campaign.dto.ParticipantDTO;
import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    public List<Campaign> findAll() {
        return campaignRepository.findAll();
    }

    public Optional<Campaign> findById(Long id) {
        return campaignRepository.findById(id);
    }

    public Campaign save(Campaign campaign) {
        return campaignRepository.save(campaign);
    }

    public void deleteById(Long id) {
        campaignRepository.deleteById(id);
    }

    @Transactional(readOnly = true) // Only read operations
    public List<CampaignDTO> getAllCampaigns() {
        return campaignRepository.findAll().stream()
                .map(campaign -> new CampaignDTO(
                        campaign.getId(),
                        campaign.getName(),
                        campaign.getDescription(),
                        // Bygg deltagarlistan om det behövs
                        campaign.getParticipants().stream()
                                .map(participant -> new ParticipantDTO(
                                        participant.getUser().getId(),
                                        participant.getUser().getUserName()
                                ))
                                .toList()
                ))
                .toList();
    }


}