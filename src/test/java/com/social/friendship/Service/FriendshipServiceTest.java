package com.social.friendship.Service;

import com.social.friendship.application.FriendshipService;
import com.social.friendship.cache.FriendshipCacheService;
import com.social.friendship.domain.DTO.event.FriendAcceptedEvent;
import com.social.friendship.domain.DTO.event.FriendRemovedEvent;
import com.social.friendship.domain.DTO.event.FriendRequestSentEvent;
import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.Friendship;
import com.social.friendship.domain.model.FriendshipStatus;
import com.social.friendship.domain.repository.FriendshipRepository;
import com.social.friendship.exception.FriendshipAlreadyExistsException;
import com.social.friendship.exception.FriendshipNotFoundException;
import com.social.friendship.mapper.FriendshipMapper;
import com.social.friendship.messaging.KafkaProducer;
import com.social.friendship.outbox.OutboxEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendshipCacheService friendshipCacheService;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private FriendshipMapper friendshipMapper;

    @InjectMocks
    private FriendshipService friendshipService;


    @Test
    void findFriendship_shouldReturnFriendship() {
        UUID requesterId = UUID.randomUUID();
        UUID addressedId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(requesterId)
                .addresseeId(addressedId)
                .status(FriendshipStatus.PENDING)
                .build();

        when(friendshipRepository.findByRequesterIdAndAddresseeId(
                requesterId,
                addressedId
        )).thenReturn(Optional.of(friendship));

        Friendship result =
                friendshipService.findFriendship(requesterId, addressedId);

        assertSame(friendship, result);

        verify(friendshipRepository)
                .findByRequesterIdAndAddresseeId(
                        requesterId,
                        addressedId
                );
    }

    @Test
    void findFriendship_shouldThrowExceptionWhenFriendshipNotFound() {
        UUID requesterId = UUID.randomUUID();
        UUID addressedId = UUID.randomUUID();

        when(friendshipRepository.findByRequesterIdAndAddresseeId(
                requesterId,
                addressedId
        )).thenReturn(Optional.empty());

        assertThrows(
                FriendshipNotFoundException.class,
                () -> friendshipService.findFriendship(
                        requesterId,
                        addressedId
                )
        );
    }


    @Test
    void sendRequest_shouldCreatePendingFriendship() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(friendshipCacheService.isFriend(
                currentUserId,
                targetUserId
        )).thenReturn(false);

        when(friendshipRepository.findBetweenUsers(
                eq(currentUserId),
                eq(targetUserId),
                anyCollection()
        )).thenReturn(Optional.empty());

        friendshipService.sendRequest(currentUserId, targetUserId);

        ArgumentCaptor<Friendship> captor =
                ArgumentCaptor.forClass(Friendship.class);

        verify(friendshipRepository).save(captor.capture());

        Friendship savedFriendship = captor.getValue();

        assertEquals(
                currentUserId,
                savedFriendship.getRequesterId()
        );

        assertEquals(
                targetUserId,
                savedFriendship.getAddresseeId()
        );

        assertEquals(
                FriendshipStatus.PENDING,
                savedFriendship.getStatus()
        );

        verify(friendshipCacheService)
                .cacheFriendship(
                        currentUserId,
                        targetUserId,
                        FriendshipStatus.PENDING
                );

        verify(outboxEventService)
                .saveEvent(
                        "friend.requested",
                        new FriendRequestSentEvent(
                                currentUserId,
                                targetUserId
                        )
                );
    }

    @Test
    void sendRequest_shouldThrowWhenUsersAreSame() {
        UUID userId = UUID.randomUUID();

        assertThrows(
                IllegalStateException.class,
                () -> friendshipService.sendRequest(
                        userId,
                        userId
                )
        );

        verifyNoInteractions(
                friendshipRepository,
                friendshipCacheService,
                kafkaProducer
        );
    }

    @Test
    void sendRequest_shouldThrowWhenUsersAlreadyFriends() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(friendshipCacheService.isFriend(
                currentUserId,
                targetUserId
        )).thenReturn(true);

        assertThrows(
                FriendshipAlreadyExistsException.class,
                () -> friendshipService.sendRequest(
                        currentUserId,
                        targetUserId
                )
        );

        verify(friendshipCacheService)
                .isFriend(currentUserId, targetUserId);

        verifyNoInteractions(
                friendshipRepository,
                kafkaProducer
        );
    }

    @Test
    void sendRequest_shouldThrowWhenRequestAlreadyPending() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.PENDING)
                .build();

        when(friendshipCacheService.isFriend(
                currentUserId,
                targetUserId
        )).thenReturn(false);

        when(friendshipRepository.findBetweenUsers(
                eq(currentUserId),
                eq(targetUserId),
                anyCollection()
        )).thenReturn(Optional.of(friendship));

        assertThrows(
                FriendshipAlreadyExistsException.class,
                () -> friendshipService.sendRequest(
                        currentUserId,
                        targetUserId
                )
        );

        verify(friendshipRepository, never()).save(any());

        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void sendRequest_shouldThrowWhenFriendshipAlreadyAccepted() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        when(friendshipCacheService.isFriend(
                currentUserId,
                targetUserId
        )).thenReturn(false);

        when(friendshipRepository.findBetweenUsers(
                eq(currentUserId),
                eq(targetUserId),
                anyCollection()
        )).thenReturn(Optional.of(friendship));

        assertThrows(
                FriendshipAlreadyExistsException.class,
                () -> friendshipService.sendRequest(
                        currentUserId,
                        targetUserId
                )
        );

        verify(friendshipRepository, never()).save(any());
        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void sendRequest_shouldChangeRejectedToPending() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.REJECTED)
                .build();

        when(friendshipCacheService.isFriend(
                currentUserId,
                targetUserId
        )).thenReturn(false);

        when(friendshipRepository.findBetweenUsers(
                eq(currentUserId),
                eq(targetUserId),
                anyCollection()
        )).thenReturn(Optional.of(friendship));

        friendshipService.sendRequest(
                currentUserId,
                targetUserId
        );

        assertEquals(
                FriendshipStatus.PENDING,
                friendship.getStatus()
        );

        verify(friendshipRepository).save(friendship);

        verify(friendshipCacheService)
                .cacheFriendship(
                        currentUserId,
                        targetUserId,
                        FriendshipStatus.PENDING
                );

        verify(kafkaProducer)
                .sendFriendRequestEvent(
                        new FriendRequestSentEvent(
                                currentUserId,
                                targetUserId
                        )
                );
    }

    @Test
    void sendRequest_shouldChangeRemovedToPending() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.REMOVED)
                .build();

        when(friendshipCacheService.isFriend(
                currentUserId,
                targetUserId
        )).thenReturn(false);

        when(friendshipRepository.findBetweenUsers(
                eq(currentUserId),
                eq(targetUserId),
                anyCollection()
        )).thenReturn(Optional.of(friendship));

        friendshipService.sendRequest(
                currentUserId,
                targetUserId
        );

        assertEquals(
                FriendshipStatus.PENDING,
                friendship.getStatus()
        );

        verify(friendshipRepository).save(friendship);

        verify(kafkaProducer)
                .sendFriendRequestEvent(
                        new FriendRequestSentEvent(
                                currentUserId,
                                targetUserId
                        )
                );
    }


    @Test
    void acceptRequest_shouldAcceptPendingFriendship() {
        UUID currentUserId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(requesterId)
                .addresseeId(currentUserId)
                .status(FriendshipStatus.PENDING)
                .build();

        when(friendshipRepository
                .findByRequesterIdAndAddresseeIdAndStatus(
                        requesterId,
                        currentUserId,
                        FriendshipStatus.PENDING
                ))
                .thenReturn(Optional.of(friendship));

        friendshipService.acceptRequest(
                currentUserId,
                requesterId
        );

        assertEquals(
                FriendshipStatus.ACCEPTED,
                friendship.getStatus()
        );

        verify(friendshipRepository).save(friendship);

        verify(friendshipCacheService)
                .deleteFriendsCache(requesterId);

        verify(friendshipCacheService)
                .deleteFriendsCache(currentUserId);

        verify(friendshipCacheService)
                .cacheFriendship(
                        requesterId,
                        currentUserId,
                        FriendshipStatus.ACCEPTED
                );

        verify(kafkaProducer)
                .sendFriendAcceptedEvent(
                        new FriendAcceptedEvent(
                                requesterId,
                                currentUserId
                        )
                );
    }

    @Test
    void acceptRequest_shouldThrowWhenPendingFriendshipNotFound() {
        UUID currentUserId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(friendshipRepository
                .findByRequesterIdAndAddresseeIdAndStatus(
                        requesterId,
                        currentUserId,
                        FriendshipStatus.PENDING
                ))
                .thenReturn(Optional.empty());

        assertThrows(
                FriendshipNotFoundException.class,
                () -> friendshipService.acceptRequest(
                        currentUserId,
                        requesterId
                )
        );

        verify(friendshipRepository, never()).save(any());
        verifyNoInteractions(
                friendshipCacheService,
                kafkaProducer
        );
    }


    @Test
    void rejectRequest_shouldRejectPendingFriendship() {
        UUID currentUserId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(requesterId)
                .addresseeId(currentUserId)
                .status(FriendshipStatus.PENDING)
                .build();

        when(friendshipRepository
                .findByRequesterIdAndAddresseeIdAndStatus(
                        requesterId,
                        currentUserId,
                        FriendshipStatus.PENDING
                ))
                .thenReturn(Optional.of(friendship));

        friendshipService.rejectRequest(
                currentUserId,
                requesterId
        );

        assertEquals(
                FriendshipStatus.REJECTED,
                friendship.getStatus()
        );

        verify(friendshipRepository).save(friendship);

        verifyNoInteractions(
                friendshipCacheService,
                kafkaProducer
        );
    }

    @Test
    void rejectRequest_shouldThrowWhenPendingFriendshipNotFound() {
        UUID currentUserId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(friendshipRepository
                .findByRequesterIdAndAddresseeIdAndStatus(
                        requesterId,
                        currentUserId,
                        FriendshipStatus.PENDING
                ))
                .thenReturn(Optional.empty());

        assertThrows(
                FriendshipNotFoundException.class,
                () -> friendshipService.rejectRequest(
                        currentUserId,
                        requesterId
                )
        );

        verify(friendshipRepository, never()).save(any());
    }


    @Test
    void removeFriend_shouldRemoveAcceptedFriendship() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(targetUserId)
                .addresseeId(currentUserId)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        when(friendshipRepository.findBetweenUsers(
                eq(targetUserId),
                eq(currentUserId),
                anyCollection()
        )).thenReturn(Optional.of(friendship));

        friendshipService.removeFriend(
                currentUserId,
                targetUserId
        );

        assertEquals(
                FriendshipStatus.REMOVED,
                friendship.getStatus()
        );

        verify(friendshipRepository).save(friendship);

        verify(friendshipCacheService)
                .deleteFriendsCache(currentUserId);

        verify(friendshipCacheService)
                .deleteFriendsCache(targetUserId);

        verify(friendshipCacheService)
                .removeFriendship(
                        currentUserId,
                        targetUserId
                );

        verify(kafkaProducer)
                .sendFriendRemovedEvent(
                        new FriendRemovedEvent(
                                currentUserId,
                                targetUserId
                        )
                );
    }

    @Test
    void removeFriend_shouldThrowWhenFriendshipNotFound() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(friendshipRepository.findBetweenUsers(
                eq(targetUserId),
                eq(currentUserId),
                anyCollection()
        )).thenReturn(Optional.empty());

        assertThrows(
                FriendshipNotFoundException.class,
                () -> friendshipService.removeFriend(
                        currentUserId,
                        targetUserId
                )
        );

        verify(friendshipRepository, never()).save(any());

        verifyNoInteractions(
                friendshipCacheService,
                kafkaProducer
        );
    }


    @Test
    void getFriends_shouldReturnCachedFriends() {
        UUID currentUserId = UUID.randomUUID();

        List<FriendResponse> friends = List.of(
                new FriendResponse(
                        UUID.randomUUID(),
                        FriendshipStatus.ACCEPTED
                )
        );

        when(friendshipCacheService.getFriends(currentUserId))
                .thenReturn(friends);

        List<FriendResponse> result =
                friendshipService.getFriends(currentUserId);

        assertSame(friends, result);

        verify(friendshipCacheService)
                .getFriends(currentUserId);

        verifyNoInteractions(
                friendshipRepository,
                friendshipMapper
        );
    }

    @Test
    void getFriends_shouldLoadFromDatabaseWhenCacheMiss() {
        UUID currentUserId = UUID.randomUUID();

        List<Friendship> friendships = List.of(
                Friendship.builder()
                        .requesterId(currentUserId)
                        .addresseeId(UUID.randomUUID())
                        .status(FriendshipStatus.ACCEPTED)
                        .build()
        );

        List<FriendResponse> responses = List.of(
                new FriendResponse(
                        UUID.randomUUID(),
                        FriendshipStatus.ACCEPTED
                )
        );

        when(friendshipCacheService.getFriends(currentUserId))
                .thenReturn(null);

        when(friendshipRepository.findByUserIdAndStatus(
                currentUserId,
                FriendshipStatus.ACCEPTED
        )).thenReturn(friendships);

        when(friendshipMapper.toFriendResponseList(
                friendships,
                currentUserId
        )).thenReturn(responses);

        List<FriendResponse> result =
                friendshipService.getFriends(currentUserId);

        assertSame(responses, result);

        verify(friendshipRepository)
                .findByUserIdAndStatus(
                        currentUserId,
                        FriendshipStatus.ACCEPTED
                );

        verify(friendshipMapper)
                .toFriendResponseList(
                        friendships,
                        currentUserId
                );

        verify(friendshipCacheService)
                .cacheFriendsList(
                        currentUserId,
                        responses
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

        when(friendshipRepository.findBetweenUsers(
                eq(currentUserId),
                eq(targetUserId),
                anyCollection()
        )).thenReturn(Optional.of(friendship));

        FriendshipStatus result =
                friendshipService.getStatus(
                        currentUserId,
                        targetUserId
                );

        assertEquals(
                FriendshipStatus.ACCEPTED,
                result
        );
    }

    @Test
    void getStatus_shouldThrowWhenFriendshipNotFound() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(friendshipRepository.findBetweenUsers(
                eq(currentUserId),
                eq(targetUserId),
                anyCollection()
        )).thenReturn(Optional.empty());

        assertThrows(
                FriendshipNotFoundException.class,
                () -> friendshipService.getStatus(
                        currentUserId,
                        targetUserId
                )
        );
    }


    @Test
    void areFriends_shouldReturnTrueWhenCacheSaysAccepted() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(friendshipCacheService.isFriend(
                currentUserId,
                targetUserId
        )).thenReturn(true);

        boolean result =
                friendshipService.areFriends(
                        currentUserId,
                        targetUserId
                );

        assertTrue(result);

        verify(friendshipCacheService)
                .isFriend(currentUserId, targetUserId);

        verifyNoInteractions(friendshipRepository);
    }

    @Test
    void areFriends_shouldReturnTrueFromDatabaseAndCacheResult() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        Friendship friendship = Friendship.builder()
                .requesterId(currentUserId)
                .addresseeId(targetUserId)
                .status(FriendshipStatus.ACCEPTED)
                .build();

        when(friendshipCacheService.isFriend(
                currentUserId,
                targetUserId
        )).thenReturn(false);

        when(friendshipRepository.findBetweenUsers(
                eq(currentUserId),
                eq(targetUserId),
                anyCollection()
        )).thenReturn(Optional.of(friendship));

        boolean result =
                friendshipService.areFriends(
                        currentUserId,
                        targetUserId
                );

        assertTrue(result);

        verify(friendshipCacheService)
                .cacheFriendship(
                        currentUserId,
                        targetUserId,
                        FriendshipStatus.ACCEPTED
                );
    }

    @Test
    void areFriends_shouldReturnFalseWhenFriendshipNotFound() {
        UUID currentUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        when(friendshipCacheService.isFriend(
                currentUserId,
                targetUserId
        )).thenReturn(false);

        when(friendshipRepository.findBetweenUsers(
                eq(currentUserId),
                eq(targetUserId),
                anyCollection()
        )).thenReturn(Optional.empty());

        boolean result =
                friendshipService.areFriends(
                        currentUserId,
                        targetUserId
                );

        assertFalse(result);

        verify(friendshipCacheService, never())
                .cacheFriendship(
                        any(),
                        any(),
                        any()
                );
    }
}
