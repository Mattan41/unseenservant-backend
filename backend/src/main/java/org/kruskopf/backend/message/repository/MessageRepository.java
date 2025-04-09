package org.kruskopf.backend.message.repository;

import org.kruskopf.backend.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByCampaignId(Long campaignId);

}
