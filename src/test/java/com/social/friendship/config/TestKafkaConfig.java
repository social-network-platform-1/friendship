package com.social.friendship.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestKafkaConfig {

    @Bean
    public NewTopic friendRequestedTopic() {
        return new NewTopic(
                "friend.requested",
                1,
                (short) 1
        );
    }

    @Bean
    public NewTopic friendAcceptedTopic() {
        return new NewTopic(
                "friend.accepted",
                1,
                (short) 1
        );
    }

    @Bean
    public NewTopic friendRemovedTopic() {
        return new NewTopic(
                "friend.removed",
                1,
                (short) 1
        );
    }
}
