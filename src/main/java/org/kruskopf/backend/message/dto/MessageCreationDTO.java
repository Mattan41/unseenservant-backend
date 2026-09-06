package org.kruskopf.backend.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageCreationDTO(
        Long campaignId,
        
        @NotBlank(message = "Message body cannot be blank")
        @Size(max = 10000, message = "Message body must not exceed 10000 characters")
        String messageBody
) {
}
