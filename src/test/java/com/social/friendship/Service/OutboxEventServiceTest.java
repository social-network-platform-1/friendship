package com.social.friendship.Service;

import com.social.friendship.domain.DTO.event.FriendRequestSentEvent;
import com.social.friendship.domain.repository.OutboxEventRepository;
import com.social.friendship.outbox.OutboxEvent;
import com.social.friendship.outbox.OutboxEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxEventService outboxEventService;

    @Test
    void saveEvent_shouldSaveOutboxEvent() {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        FriendRequestSentEvent event =
                new FriendRequestSentEvent(
                        userId,
                        targetId
                );

        String payload =
                "{\"userId\":\"" + userId +
                        "\",\"targetId\":\"" + targetId + "\"}";

        when(objectMapper.writeValueAsString(event))
                .thenReturn(payload);

        outboxEventService.saveEvent(
                "friend.requested",
                event
        );

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository)
                .save(captor.capture());

        OutboxEvent savedEvent =
                captor.getValue();

        assertEquals(
                "friend.requested",
                savedEvent.getTopic()
        );

        assertEquals(
                "FriendRequestSentEvent",
                savedEvent.getEventType()
        );

        assertEquals(
                payload,
                savedEvent.getPayload()
        );

        assertNotNull(savedEvent.getCreatedAt());

        assertFalse(savedEvent.isProcessed());

        verify(objectMapper)
                .writeValueAsString(event);
    }
}
