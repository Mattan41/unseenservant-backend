package org.kruskopf.backend.message.service;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.campaign.service.CampaignPermissionService;
import org.kruskopf.backend.exception.ResourceNotFoundException;
import org.kruskopf.backend.exception.UnauthorizedAccessException;
import org.kruskopf.backend.message.MessageMapper;
import org.kruskopf.backend.message.dto.MessageCreationDTO;
import org.kruskopf.backend.message.dto.MessageDTO;
import org.kruskopf.backend.message.entity.Message;
import org.kruskopf.backend.message.repository.MessageRepository;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignPermissionService campaignPermissionService;
    private final MessageMapper messageMapper;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository, CampaignRepository campaignRepository, CampaignPermissionService campaignPermissionService, MessageMapper messageMapper) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
        this.campaignPermissionService = campaignPermissionService;
        this.messageMapper = messageMapper;
    }

    public List<MessageDTO> getMessagesByCampaignId(Long campaignId, long userId) {
        if (!campaignRepository.existsById(campaignId)) {
            throw new ResourceNotFoundException("Campaign not found with id: " + campaignId);
        }

        if (!campaignPermissionService.isParticipant(campaignId, userId)) {
            throw new UnauthorizedAccessException("User is not a participant in this campaign");
        }

        return messageRepository.findByCampaignId(campaignId).stream()
                .map(messageMapper::toDTO)
                .toList();
    }

    public MessageDTO getMessageById(Long id, long userId) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + id));

        if (!campaignPermissionService.isParticipant(message.getCampaign().getId(), userId)) {
            throw new UnauthorizedAccessException("User is not a participant in this campaign");
        }

        return messageMapper.toDTO(message);
    }

    @Transactional
    public MessageDTO createMessage(MessageCreationDTO dto, long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Campaign campaign = campaignRepository.findById(dto.campaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + dto.campaignId()));

        if (!campaignPermissionService.isParticipant(dto.campaignId(), userId)) {
            throw new UnauthorizedAccessException("User is not a participant in this campaign");
        }

        Message message = messageMapper.toEntity(dto, user);
        message.setCampaign(campaign);
        Message savedMessage = messageRepository.save(message);

        return messageMapper.toDTO(savedMessage);
    }

    @Transactional
    public void deleteMessage(Long id, long userId) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + id));

        // Only the message sender can delete their own message
        // Design decision: GM cannot delete other users' messages to keep authorization simple
        if (!message.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("User is not the owner of this message");
        }

        messageRepository.delete(message);
    }
}