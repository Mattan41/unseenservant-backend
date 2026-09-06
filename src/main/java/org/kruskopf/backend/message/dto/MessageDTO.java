package org.kruskopf.backend.message.dto;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public record MessageDTO(
        Long id,
        Long campaignId,
        Long userId,
        String messageBody,
        LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt
) {
}