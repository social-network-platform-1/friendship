package com.social.friendship.Service;

import com.social.friendship.application.FriendshipService;
import com.social.friendship.cache.FriendshipCacheService;
import com.social.friendship.domain.DTO.event.FriendAcceptedEvent;
import com.social.friendship.domain.DTO.event.FriendRequestSentEvent;
import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.Friendship;
import com.social.friendship.domain.model.FriendshipStatus;
import com.social.friendship.domain.repository.FriendshipRepository;
import com.social.friendship.exception.FriendshipNotFoundException;
import com.social.friendship.messaging.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private FriendshipCacheService  friendshipCacheService;
    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private FriendshipService friendshipService;

    private UUID userId;
    private  UUID targetUserId;
    private Friendship friendship;

    @BeforeEach
    public void setup() {
        userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        targetUserId = UUID.fromString("000e0000-e29b-41d4-a716-446655440000");
        friendship = new Friendship(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), userId, UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), FriendshipStatus.ACCEPTED, Instant.now(), Instant.now());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "550e8400-e29b-41d4-a716-446655440000",
                null,
                List.of()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void findFriendshipsTest() {
        when(friendshipRepository.findByRequesterId(userId))
                .thenReturn(Optional.of(friendship));
        Friendship friendship1 = friendshipService.findFriendship(userId);
        verify(friendshipRepository).findByAddresseeId(userId);
        assertEquals(friendship, friendship1);

    }

    @Test
    void findFriendshipsNotFoundTest() {
        when(friendshipRepository.findByRequesterId(userId))
                .thenReturn(Optional.empty());
        assertThrows(FriendshipNotFoundException.class,
                () -> friendshipService.findFriendship(userId));
        verify(friendshipRepository).findByRequesterId(userId);
    }

    @Test
    void sendFriendshipInCacheTest() {
        when(friendshipCacheService.isFriend(userId, targetUserId))
                .thenReturn(true);
        assertThrows(RuntimeException.class,
                ()-> friendshipService.sendRequest(targetUserId));
        verify(friendshipCacheService).isFriend(userId, targetUserId);
    }

    @Test
    void sendFriendshipNotFoundTest() {
        when(friendshipCacheService.isFriend(userId, targetUserId))
                .thenReturn(false);
        when(friendshipRepository.findByRequesterIdAndAddresseeId(userId, targetUserId))
                .thenReturn(Optional.empty());
        assertThrows(FriendshipNotFoundException.class,
                () -> friendshipService.sendRequest(targetUserId));
        verify(friendshipCacheService).isFriend(userId, targetUserId);
    }

    @Test
    void sendFriendshipTest() {
        when(friendshipCacheService.isFriend(userId, targetUserId))
                .thenReturn(false);
        when(friendshipRepository.findByRequesterIdAndAddresseeId(userId, targetUserId))
                .thenReturn(Optional.of(friendship));
        friendshipService.sendRequest(targetUserId);
        verify(friendshipCacheService).isFriend(userId, targetUserId);
        verify(friendshipCacheService).cacheFriendship(userId, targetUserId, FriendshipStatus.PENDING);
        verify(kafkaProducer).sendFriendRequestEvent(new FriendRequestSentEvent(userId, targetUserId));
    }

    @Test
    void acceptRequestTest() {
       UUID requesterID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
       UUID addressedId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

       Friendship friendship1 = Friendship.builder()
               .id(UUID.randomUUID())
               .requesterId(requesterID)
               .addresseeId(addressedId)
               .createdAt(Instant.now())
               .status(FriendshipStatus.PENDING)
               .updatedAt(Instant.now())
               .build();

        when(friendshipRepository.findByRequesterIdAndAddresseeId(requesterID, addressedId))
                .thenReturn(Optional.of(friendship1));

        friendshipService.acceptRequest(addressedId);

        assertEquals(FriendshipStatus.ACCEPTED, friendship1.getStatus());
        verify(friendshipRepository).save(friendship1);
        verify(friendshipCacheService)
                .cacheFriendship(requesterID, addressedId, FriendshipStatus.ACCEPTED);
        verify(kafkaProducer)
                .sendFriendAcceptedEvent(any());
    }

    @Test
    void rejectRequestTest() {
        UUID requesterID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID addressedId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        Friendship friendship1 = Friendship.builder()
                .id(UUID.randomUUID())
                .requesterId(requesterID)
                .addresseeId(addressedId)
                .createdAt(Instant.now())
                .status(FriendshipStatus.PENDING)
                .updatedAt(Instant.now())
                .build();

        when(friendshipRepository.findByRequesterIdAndAddresseeId(requesterID, addressedId))
                .thenReturn(Optional.of(friendship1));
        friendshipService.rejectRequest(addressedId);
        verify(friendshipRepository).save(friendship1);
    }

    @Test
    void removeFriendshipTest() {
        UUID requesterID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID addressedId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        Friendship friendship1 = Friendship.builder()
                .id(UUID.randomUUID())
                .requesterId(requesterID)
                .addresseeId(addressedId)
                .createdAt(Instant.now())
                .status(FriendshipStatus.PENDING)
                .updatedAt(Instant.now())
                .build();

        when(friendshipRepository.findByRequesterIdAndAddresseeId(requesterID, addressedId))
                .thenReturn(Optional.of(friendship1));
        friendshipService.removeFriend(addressedId);
        verify(friendshipRepository).save(friendship1);
        verify(friendshipCacheService).removeFriendship(any(), any());
        verify(kafkaProducer).sendFriendRemovedEvent(any());
    }

    @Test
    void getFriendshipsTest() {
        UUID userId =  UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        FriendResponse friendResponse = FriendResponse.builder()
                .userId(UUID.randomUUID())
                .status(FriendshipStatus.ACCEPTED)
                .build();

        UUID requesterID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID addressedId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        Friendship friendship1 = Friendship.builder()
                .id(UUID.randomUUID())
                .requesterId(requesterID)
                .addresseeId(addressedId)
                .createdAt(Instant.now())
                .status(FriendshipStatus.PENDING)
                .updatedAt(Instant.now())
                .build();

        List<FriendResponse> friendResponseList = null;
        List<Friendship> friendshipList = new ArrayList<>();
        friendshipList.add(friendship1);
        friendshipList.add(friendship1);

        when(friendshipCacheService.getFriends(userId))
                .thenReturn(friendResponseList);

        when(friendshipRepository.findFriends(userId,FriendshipStatus.ACCEPTED))
                .thenReturn(friendshipList);

        friendshipService.getFriends();

        verify(friendshipCacheService).getFriends(userId);
        verify(friendshipRepository).findFriends(userId,FriendshipStatus.ACCEPTED);

    }
}
