package com.social.friendship.redis;

import com.social.friendship.cache.FriendshipCacheService;
import com.social.friendship.domain.DTO.response.FriendResponse;
import com.social.friendship.domain.model.FriendshipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendshipCacheServiceTest {

    @Mock
    private RedisTemplate<String, FriendshipStatus> statusRedisTemplate;

    @Mock
    private RedisTemplate<String, List<FriendResponse>> responseRedisTemplate;

    @Mock
    private ValueOperations<String, FriendshipStatus> statusValueOperations;

    @Mock
    private ValueOperations<String, List<FriendResponse>> responseValueOperations;

    private FriendshipCacheService friendshipCacheService;

    @BeforeEach
    void setUp() {
        friendshipCacheService = new FriendshipCacheService(
                statusRedisTemplate,
                responseRedisTemplate
        );
    }


    @Test
    void isFriend_shouldReturnTrueWhenStatusIsAccepted() {
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        String key = "friendship:" + userId + ":" + otherId;

        when(statusRedisTemplate.opsForValue())
                .thenReturn(statusValueOperations);

        when(statusValueOperations.get(key))
                .thenReturn(FriendshipStatus.ACCEPTED);

        boolean result =
                friendshipCacheService.isFriend(userId, otherId);

        assertTrue(result);

        verify(statusValueOperations).get(key);
    }


    @Test
    void isFriend_shouldReturnFalseWhenStatusIsPending() {
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        String key = "friendship:" + userId + ":" + otherId;

        when(statusRedisTemplate.opsForValue())
                .thenReturn(statusValueOperations);

        when(statusValueOperations.get(key))
                .thenReturn(FriendshipStatus.PENDING);

        boolean result =
                friendshipCacheService.isFriend(userId, otherId);

        assertFalse(result);

        verify(statusValueOperations).get(key);
    }


    @Test
    void isFriend_shouldReturnFalseWhenStatusIsNull() {
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        String key = "friendship:" + userId + ":" + otherId;

        when(statusRedisTemplate.opsForValue())
                .thenReturn(statusValueOperations);

        when(statusValueOperations.get(key))
                .thenReturn(null);

        boolean result =
                friendshipCacheService.isFriend(userId, otherId);

        assertFalse(result);

        verify(statusValueOperations).get(key);
    }


    @Test
    void cacheFriendship_shouldCacheStatusForBothDirections() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        String key1 = "friendship:" + userA + ":" + userB;
        String key2 = "friendship:" + userB + ":" + userA;

        when(statusRedisTemplate.opsForValue())
                .thenReturn(statusValueOperations);

        friendshipCacheService.cacheFriendship(
                userA,
                userB,
                FriendshipStatus.ACCEPTED
        );

        verify(statusValueOperations)
                .set(key1, FriendshipStatus.ACCEPTED);

        verify(statusValueOperations)
                .set(key2, FriendshipStatus.ACCEPTED);
    }


    @Test
    void removeFriendship_shouldDeleteBothDirections() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        String key1 = "friendship:" + userA + ":" + userB;
        String key2 = "friendship:" + userB + ":" + userA;

        friendshipCacheService.removeFriendship(userA, userB);

        verify(statusRedisTemplate).delete(key1);
        verify(statusRedisTemplate).delete(key2);
    }


    @Test
    void getFriends_shouldReturnCachedFriends() {
        UUID userId = UUID.randomUUID();

        List<FriendResponse> friends = List.of(
                new FriendResponse(
                        UUID.randomUUID(),
                        FriendshipStatus.ACCEPTED
                ),
                new FriendResponse(
                        UUID.randomUUID(),
                        FriendshipStatus.ACCEPTED
                )
        );

        String key = "friendships:" + userId;

        when(responseRedisTemplate.opsForValue())
                .thenReturn(responseValueOperations);

        when(responseValueOperations.get(key))
                .thenReturn(friends);

        List<FriendResponse> result =
                friendshipCacheService.getFriends(userId);

        assertSame(friends, result);

        verify(responseValueOperations).get(key);
    }


    @Test
    void getFriends_shouldReturnNullWhenCacheIsEmpty() {
        UUID userId = UUID.randomUUID();

        String key = "friendships:" + userId;

        when(responseRedisTemplate.opsForValue())
                .thenReturn(responseValueOperations);

        when(responseValueOperations.get(key))
                .thenReturn(null);

        List<FriendResponse> result =
                friendshipCacheService.getFriends(userId);

        assertNull(result);

        verify(responseValueOperations).get(key);
    }


    @Test
    void deleteFriendsCache_shouldDeleteUserFriendsCache() {
        UUID userId = UUID.randomUUID();

        String key = "friendships:" + userId;

        friendshipCacheService.deleteFriendsCache(userId);

        verify(responseRedisTemplate).delete(key);
    }


    @Test
    void cacheFriendsList_shouldSaveFriendsList() {
        UUID userId = UUID.randomUUID();

        List<FriendResponse> friends = List.of(
                new FriendResponse(
                        UUID.randomUUID(),
                        FriendshipStatus.ACCEPTED
                )
        );

        String key = "friendships:" + userId;

        when(responseRedisTemplate.opsForValue())
                .thenReturn(responseValueOperations);

        friendshipCacheService.cacheFriendsList(
                userId,
                friends
        );

        verify(responseValueOperations)
                .set(key, friends);
    }
}
