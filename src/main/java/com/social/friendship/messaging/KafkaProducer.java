package com.social.friendship.messaging;

import com.social.friendship.domain.DTO.event.FriendAcceptedEvent;
import com.social.friendship.domain.DTO.event.FriendRemovedEvent;
import com.social.friendship.domain.DTO.event.FriendRequestSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendFriendRequestEvent(FriendRequestSentEvent event) {
        kafkaTemplate.send("friend.requested", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send friend-request event", ex);
                    } else {
                        log.info("Friend-request event sent, offset={}",
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void sendFriendAcceptedEvent(FriendAcceptedEvent event) {
        kafkaTemplate.send("friend.accepted", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send friend-accepted event", ex);
                    }  else {
                        log.info("Friend-accepted event sent, offset={}", result.getRecordMetadata().offset());
                    }
                });
    }

    public void sendFriendRemovedEvent(FriendRemovedEvent event) {
        kafkaTemplate.send("friend.removed", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send friend-removed event", ex);
                    }  else {
                        log.info("Friend-removed event sent, offset={}", result.getRecordMetadata().offset());
                    }
                });
    }
}
