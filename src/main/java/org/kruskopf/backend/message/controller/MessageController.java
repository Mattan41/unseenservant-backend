package org.kruskopf.backend.message.controller;

import jakarta.validation.Valid;
import org.kruskopf.backend.message.dto.MessageCreationDTO;
import org.kruskopf.backend.message.dto.MessageDTO;
import org.kruskopf.backend.message.service.MessageService;
import org.kruskopf.backend.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/campaign/{campaignId}")
    public List<MessageDTO> getMessagesByCampaignId(@PathVariable Long campaignId,
                                                    @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return messageService.getMessagesByCampaignId(campaignId, userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageDTO> getMessageById(@PathVariable Long id,
                                                    @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        return ResponseEntity.ok(messageService.getMessageById(id, userId));
    }

    @PostMapping
    public ResponseEntity<MessageDTO> createMessage(@Valid @RequestBody MessageCreationDTO dto,
                                                     @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        MessageDTO createdMessage = messageService.createMessage(dto, userId);
        return ResponseEntity.ok(createdMessage);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id,
                                              @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.user().getId();
        messageService.deleteMessage(id, userId);
        return ResponseEntity.noContent().build();
    }
}