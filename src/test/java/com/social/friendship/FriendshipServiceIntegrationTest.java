package com.social.friendship;

import com.social.friendship.application.FriendshipService;
import com.social.friendship.cache.FriendshipCacheService;
import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.Friendship;
import com.social.friendship.domain.model.FriendshipStatus;
import com.social.friendship.domain.repository.FriendshipRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FriendshipServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private FriendshipCacheService friendshipCacheService;

    @Autowired
    private RedisTemplate<String, FriendshipStatus> statusRedisTemplate;

    private Consumer<String, String> consumer;


    @BeforeEach
    void setUpKafkaConsumer() {
        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "friendship-test-" + UUID.randomUUID()
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(List.of("friend.requested"));
    }


    @AfterEach
    void tearDownKafkaConsumer() {
        consumer.close();
    }


    @Test
    void sendRequest_shouldSaveFriendshipToDatabase() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        friendshipService.sendRequest(
                currentUserId,
                targetUserId
        );

        Optional<Friendship> friendship =
                friendshipRepository.findBetweenUsers(
                        currentUserId,
                        targetUserId,
                        List.of(FriendshipStatus.PENDING)
                );

        assertTrue(friendship.isPresent());

        assertEquals(
                FriendshipStatus.PENDING,
                friendship.get().getStatus()
        );
    }


    @Test
    void sendRequest_shouldSendKafkaEvent() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        friendshipService.sendRequest(
                currentUserId,
                targetUserId
        );

        ConsumerRecord<String, String> event =
                waitForKafkaEvent("friend.requested");

        assertTrue(
                event.value().contains(currentUserId.toString())
        );

        assertTrue(
                event.value().contains(targetUserId.toString())
        );
    }


    private ConsumerRecord<String, String> waitForKafkaEvent(
            String topic
    ) {
        consumer.subscribe(List.of(topic));

        long timeout = System.currentTimeMillis() + 10_000;

        while (System.currentTimeMillis() < timeout) {
            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(500));

            if (!records.isEmpty()) {
                return records.iterator().next();
            }
        }

        throw new AssertionError(
                "Kafka event was not received from topic: " + topic
        );
    }

    @Test
    void acceptRequest_shouldAcceptFriendshipAndSendEvent() {
        UUID requesterId = UUID.randomUUID();
        UUID addresseeId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(requesterId)
                .addresseeId(addresseeId)
                .status(FriendshipStatus.PENDING)
                .build();

        friendshipRepository.save(friendship);

        friendshipService.acceptRequest(
                addresseeId,
                requesterId
        );

        Friendship savedFriendship =
                friendshipRepository
                        .findByRequesterIdAndAddresseeId(
                                requesterId,
                                addresseeId
                        )
                        .orElseThrow();

        assertEquals(
                FriendshipStatus.ACCEPTED,
                savedFriendship.getStatus()
        );

        ConsumerRecord<String, String> event =
                waitForKafkaEvent("friend.accepted");

        assertTrue(
                event.value().contains(
                        requesterId.toString()
                )
        );

        assertTrue(
                event.value().contains(
                        addresseeId.toString()
                )
        );
    }

    @Test
    void rejectRequest_shouldRejectFriendship() {
        UUID requesterId = UUID.randomUUID();
        UUID addresseeId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(requesterId)
                .addresseeId(addresseeId)
                .status(FriendshipStatus.PENDING)
                .build();

        friendshipRepository.save(friendship);

        friendshipService.rejectRequest(
                addresseeId,
                requesterId
        );

        Friendship savedFriendship =
                friendshipRepository
                        .findByRequesterIdAndAddresseeId(
                                requesterId,
                                addresseeId
                        )
                        .orElseThrow();

        assertEquals(
                FriendshipStatus.REJECTED,
                savedFriendship.getStatus()
        );
    }

    @Test
    void removeFriend_shouldRemoveFriendshipAndSendEvent() {
        UUID userId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(targetUserId)
                .addresseeId(userId)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        friendshipRepository.save(friendship);

        friendshipService.removeFriend(
                userId,
                targetUserId
        );

        Friendship savedFriendship =
                friendshipRepository
                        .findByRequesterIdAndAddresseeId(
                                targetUserId,
                                userId
                        )
                        .orElseThrow();

        assertEquals(
                FriendshipStatus.REMOVED,
                savedFriendship.getStatus()
        );

        ConsumerRecord<String, String> event =
                waitForKafkaEvent("friend.removed");

        assertTrue(
                event.value().contains(
                        userId.toString()
                )
        );

        assertTrue(
                event.value().contains(
                        targetUserId.toString()
                )
        );
    }

    @Test
    void getFriends_shouldLoadFromDatabaseAndCacheResult() {
        UUID currentUserId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(friendId)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        friendshipRepository.save(friendship);

        List<FriendResponse> friends =
                friendshipService.getFriends(currentUserId);

        assertEquals(1, friends.size());

        assertEquals(
                friendId,
                friends.get(0).userId()
        );

        assertEquals(
                FriendshipStatus.ACCEPTED,
                friends.get(0).status()
        );
    }

    @Test
    void getFriends_shouldReturnFriendsFromCacheOnSecondRequest() {
        UUID currentUserId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(friendId)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        friendshipRepository.save(friendship);

        List<FriendResponse> firstResult =
                friendshipService.getFriends(currentUserId);

        assertEquals(1, firstResult.size());
        assertEquals(friendId, firstResult.get(0).userId());
        assertEquals(
                FriendshipStatus.ACCEPTED,
                firstResult.get(0).status()
        );

        List<FriendResponse> secondResult =
                friendshipService.getFriends(currentUserId);

        assertEquals(1, secondResult.size());
        assertEquals(friendId, secondResult.get(0).userId());
        assertEquals(
                FriendshipStatus.ACCEPTED,
                secondResult.get(0).status()
        );
    }

    @Test
    void getStatus_shouldReturnFriendshipStatus() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        friendshipRepository.save(friendship);

        FriendshipStatus status =
                friendshipService.getStatus(
                        currentUserId,
                        targetUserId
                );

        assertEquals(
                FriendshipStatus.ACCEPTED,
                status
        );
    }

    @Test
    void areFriends_shouldReturnTrueWhenFriendshipIsCached() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        friendshipCacheService.cacheFriendship(
                currentUserId,
                targetUserId,
                FriendshipStatus.ACCEPTED
        );

        Boolean cached =
                friendshipCacheService.isFriend(
                        currentUserId,
                        targetUserId
                );

        assertTrue(cached);

        boolean result =
                friendshipService.areFriends(
                        currentUserId,
                        targetUserId
                );

        assertTrue(result);
    }

    @Test
    void areFriends_shouldLoadFromDatabaseWhenNotCached() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        friendshipRepository.save(friendship);

        boolean result =
                friendshipService.areFriends(
                        currentUserId,
                        targetUserId
                );

        assertTrue(result);
    }

    @Test
    void areFriends_shouldReturnFalseWhenFriendshipDoesNotExist() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        boolean result =
                friendshipService.areFriends(
                        currentUserId,
                        targetUserId
                );

        assertFalse(result);
    }
}
