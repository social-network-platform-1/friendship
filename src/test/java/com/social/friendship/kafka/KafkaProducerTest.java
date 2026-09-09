package com.social.friendship.kafka;

import com.social.friendship.domain.DTO.event.FriendAcceptedEvent;
import com.social.friendship.domain.DTO.event.FriendRemovedEvent;
import com.social.friendship.domain.DTO.event.FriendRequestSentEvent;
import com.social.friendship.messaging.KafkaProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CompletableFuture<SendResult<String, Object>> future;

    @InjectMocks
    private KafkaProducer kafkaProducer;


    @Test
    void sendFriendRequestEvent_shouldSendEvent() {
        FriendRequestSentEvent event =
                new FriendRequestSentEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        when(kafkaTemplate.send(
                "friend.requested",
                event
        )).thenReturn(future);

        kafkaProducer.sendFriendRequestEvent(event);

        verify(kafkaTemplate).send(
                "friend.requested",
                event
        );
    }


    @Test
    void sendFriendAcceptedEvent_shouldSendEvent() {
        FriendAcceptedEvent event =
                new FriendAcceptedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        when(kafkaTemplate.send(
                "friend.accepted",
                event
        )).thenReturn(future);

        kafkaProducer.sendFriendAcceptedEvent(event);

        verify(kafkaTemplate).send(
                "friend.accepted",
                event
        );
    }


    @Test
    void sendFriendRemovedEvent_shouldSendEvent() {
        FriendRemovedEvent event =
                new FriendRemovedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID()
                );

        when(kafkaTemplate.send(
                "friend.removed",
                event
        )).thenReturn(future);

        kafkaProducer.sendFriendRemovedEvent(event);

        verify(kafkaTemplate).send(
                "friend.removed",
                event
        );
    }
}
