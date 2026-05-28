package org.kruskopf.backend.message.controller;

import org.kruskopf.backend.message.dto.MessageDTO;
import org.kruskopf.backend.message.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public List<MessageDTO> getAllMessages() {
        return messageService.getAllMessages();
    }

    @GetMapping("/campaign/{campaignId}")
    public List<MessageDTO> getMessagesByCampaignId(@PathVariable Long campaignId) {
        return messageService.getMessagesByCampaignId(campaignId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageDTO> getMessageById(@PathVariable Long id) {
        MessageDTO messageDTO = messageService.getMessageById(id);
        return messageDTO != null ? ResponseEntity.ok(messageDTO) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public MessageDTO createMessage(@RequestBody MessageDTO messageDTO) {
        return messageService.createMessage(messageDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        messageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }
}