package org.kruskopf.backend.message;

import org.kruskopf.backend.message.dto.MessageCreationDTO;
import org.kruskopf.backend.message.dto.MessageDTO;
import org.kruskopf.backend.message.entity.Message;
import org.kruskopf.backend.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageDTO toDTO(Message message) {
        return new MessageDTO(
                message.getId(),
                message.getCampaign() != null ? message.getCampaign().getId() : null,
                message.getUser() != null ? message.getUser().getId() : null,
                message.getMessageBody(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

    public Message toEntity(MessageCreationDTO dto, User user) {
        Message message = new Message();
        message.setMessageBody(dto.messageBody());
        message.setUser(user);
        return message;
    }
}
