package org.kruskopf.backend.message.service;

import org.kruskopf.backend.campaign.entity.Campaign;
import org.kruskopf.backend.campaign.repository.CampaignRepository;
import org.kruskopf.backend.message.dto.MessageDTO;
import org.kruskopf.backend.message.entity.Message;
import org.kruskopf.backend.message.repository.MessageRepository;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository, CampaignRepository campaignRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.campaignRepository = campaignRepository;
    }

    public List<MessageDTO> getAllMessages() {
        return messageRepository.findAll().stream().map(this::convertToDTO).toList();
    }

    public List<MessageDTO> getMessagesByCampaignId(Long campaignId) {
        return messageRepository.findByCampaignId(campaignId).stream().map(this::convertToDTO).toList();
    }

    public MessageDTO getMessageById(Long id) {
        return messageRepository.findById(id).map(this::convertToDTO).orElse(null);
    }

    public MessageDTO createMessage(MessageDTO messageDTO) {
        User user = userRepository.findById(messageDTO.userId()).orElseThrow(() -> new RuntimeException("User not found"));
        Campaign campaign = campaignRepository.findById(messageDTO.campaignId()).orElseThrow(() -> new RuntimeException("Campaign not found"));
        Message message = new Message(campaign, user, messageDTO.messageBody());
        return convertToDTO(messageRepository.save(message));
    }

    public void deleteMessage(Long id) {
        messageRepository.deleteById(id);
    }

    private MessageDTO convertToDTO(Message message) {
        return new MessageDTO(message.getCampaign().getId(), message.getUser().getId(), message.getMessageBody(), message.getCreatedAt(), message.getUpdatedAt());
    }


}