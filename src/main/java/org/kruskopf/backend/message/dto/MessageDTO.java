package org.kruskopf.backend.message.dto;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record MessageDTO(Long campaignId, Long userId, String messageBody, LocalDateTime createdAt,
                         @Nullable LocalDateTime updatedAt) {
    public MessageDTO(Long campaignId, Long userId, String messageBody) {
        this(campaignId, userId, messageBody, LocalDateTime.now(), LocalDateTime.now());
    }
}