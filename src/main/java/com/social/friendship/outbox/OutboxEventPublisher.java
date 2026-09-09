package com.social.friendship.outbox;

import com.social.friendship.domain.repository.OutboxEventRepository;
import com.social.friendship.messaging.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaProducer kafkaProducer;

    @Scheduled(fixedDelay = 1000)
    public void publishEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findByProcessedFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {

            kafkaProducer
                    .sendEvent(
                            event.getTopic(),
                            event.getPayload()
                    )
                    .whenComplete((result, exception) -> {

                        if (exception != null) {
                            log.error(
                                    "Failed to publish outbox event {}",
                                    event.getId(),
                                    exception
                            );
                            return;
                        }

                        event.setProcessed(true);

                        outboxEventRepository.save(event);

                        log.info(
                                "Outbox event {} published to topic {}",
                                event.getId(),
                                event.getTopic()
                        );
                    });
        }
    }
}
