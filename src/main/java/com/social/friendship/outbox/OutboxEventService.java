package com.social.friendship.outbox;

import com.social.friendship.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void saveEvent(
            String topic,
            Object event
    ) {
        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .topic(topic)
                .eventType(event.getClass().getSimpleName())
                .payload(payload)
                .createdAt(Instant.now())
                .processed(false)
                .build();

        outboxEventRepository.save(outboxEvent);
    }
}
